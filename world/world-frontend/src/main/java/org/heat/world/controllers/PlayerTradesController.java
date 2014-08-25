package org.heat.world.controllers;

import com.ankamagames.dofus.network.enums.DialogTypeEnum;
import com.ankamagames.dofus.network.enums.GameContextEnum;
import com.ankamagames.dofus.network.messages.game.dialog.LeaveDialogRequestMessage;
import com.ankamagames.dofus.network.messages.game.inventory.exchanges.*;
import com.ankamagames.dofus.network.messages.game.inventory.items.*;
import com.github.blackrush.acara.Listener;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fungsi.concurrent.Future;
import org.fungsi.concurrent.Promise;
import org.fungsi.concurrent.Promises;
import org.heat.world.controllers.events.EnterContextEvent;
import org.heat.world.controllers.events.roleplay.trades.AcceptPlayerTradeEvent;
import org.heat.world.controllers.events.roleplay.trades.InvitePlayerTradeEvent;
import org.heat.world.controllers.utils.Basics;
import org.heat.world.items.WorldItem;
import org.heat.world.items.WorldItemRepository;
import org.heat.world.items.WorldItemWallet;
import org.heat.world.players.Player;
import org.heat.world.players.items.PlayerItemWallet;
import org.heat.world.roleplay.WorldAction;
import org.heat.world.roleplay.environment.WorldMap;
import org.heat.world.trading.WorldTrade;
import org.heat.world.trading.WorldTradeSide;
import org.heat.world.trading.impl.player.PlayerTrade;
import org.heat.world.trading.impl.player.PlayerTradeFactory;
import org.heat.world.trading.impl.player.events.*;
import org.rocket.network.*;

import javax.inject.Inject;
import java.util.Optional;

import static com.ankamagames.dofus.network.enums.ExchangeErrorEnum.REQUEST_IMPOSSIBLE;
import static com.ankamagames.dofus.network.enums.ExchangeTypeEnum.PLAYER_TRADE;

@Slf4j
@Controller
public class PlayerTradesController {
    @Inject NetworkClient client;
    @Inject Prop<Player> player;
    @Inject MutProp<WorldAction> currentAction;

    @Inject PlayerTradeFactory tradeFactory;
    @Inject WorldItemRepository itemRepository;

    @RequiredArgsConstructor
    @Getter
    class TradeAction implements WorldAction {
        final PlayerTrade trade;
        final WorldTradeSide side;
        final Player actor = player.get();
        final WorldItemWallet wallet = actor.getWallet().createTemp();
        final Promise<WorldAction> endFuture = Promises.create();

        public WorldItemWallet getPublicWallet() {
            return trade.getTradeBag(side);
        }

        public WorldItemWallet getPrivateWallet() {
            return wallet;
        }

        @Override
        public Future<WorldAction> cancel() {
            if (!endFuture.isDone()) {
                trade.cancel(side);
                endFuture.complete(this);
            }
            return endFuture;
        }
    }

    private TradeAction getTradeAction() {
        return (TradeAction) currentAction.get();
    }

    @Listener
    public void listenTradeInvitations(EnterContextEvent evt) {
        if (evt.getContext() != GameContextEnum.ROLE_PLAY) return;

        player.get().getEventBus().subscribe(this);
    }

    @Receive
    public void invite(ExchangePlayerRequestMessage msg) {
        Player player = this.player.get();
        WorldMap map = player.getPosition().getMap();

        Optional<Player> option = map.findActor(msg.target).flatMap(Player::asPlayer);
        if (!option.isPresent()) {
            client.write(new ExchangeErrorMessage(REQUEST_IMPOSSIBLE.value));
            return;
        }
        Player target = option.get();

        PlayerTrade trade = tradeFactory.apply(player, target);

        target.getEventBus().publish(new InvitePlayerTradeEvent(trade, player))
            .filter(answers -> answers.contains(InvitePlayerTradeEvent.Ack)).toUnit()
            .onSuccess(u -> {
                currentAction.set(new TradeAction(trade, WorldTradeSide.FIRST));
                trade.getEventBus().subscribe(this);
                client.write(new ExchangeRequestedTradeMessage(
                        PLAYER_TRADE.value,
                        player.getId(),
                        target.getId()
                ));
            })
            .onFailure(err -> {
                client.write(new ExchangeErrorMessage(REQUEST_IMPOSSIBLE.value));
            });
    }

    @Receive
    public void cancel(LeaveDialogRequestMessage msg) {
        currentAction.get().cancel();
        currentAction.remove();
    }

    @Receive
    public void accept(ExchangeAcceptMessage msg) {
        TradeAction action = getTradeAction();

        if (action.side == WorldTradeSide.FIRST) {
            log.warn("no you cant force to exchange {}", client);
            client.write(Basics.noop());
            return;
        }

        action.trade.getEventBus().publish(AcceptPlayerTradeEvent.INSTANCE);
    }

    @Receive
    public void setKamas(ExchangeObjectMoveKamaMessage msg) {
        TradeAction action = getTradeAction();

        if (msg.quantity > action.getPrivateWallet().getKamas()) {
            // is this the right error value?
            client.write(new ExchangeErrorMessage(REQUEST_IMPOSSIBLE.value));
            return;
        } else if (msg.quantity < 0) {
            // is this the right error value?
            client.write(new ExchangeErrorMessage(REQUEST_IMPOSSIBLE.value));
            return;
        }

        action.trade.uncheckAllIfNeeded();
        action.getPublicWallet().setKamas(msg.quantity);
    }

    @Receive
    public void moveItem(ExchangeObjectMoveMessage msg) {
        TradeAction action = getTradeAction();

        if (msg.quantity == 0) {
            client.write(Basics.noop());
            return;
        }

        action.trade.uncheckAllIfNeeded();

        final boolean toPublic = msg.quantity > 0;
        final int quantity = Math.abs(msg.quantity);

        final WorldItemWallet wallet;
        final WorldItemWallet targetWallet;
        final WorldItem item;

        if (toPublic) {
            wallet = action.getPrivateWallet();
            targetWallet = action.getPublicWallet();
            item = wallet.findByUid(msg.objectUID).get();
        } else {
            wallet = action.getPublicWallet();
            targetWallet = action.getPrivateWallet();
            item = wallet.findByUid(msg.objectUID).get();
        }

        WorldItem itemAfterFork =
            wallet.fork(item, quantity)
                .foldRight(x -> {
                    wallet.remove(x);
                    return x;
                })
                .thenLeft(result -> {
                    wallet.update(result.first);
                    return result.second.withUid(result.first.getUid());
                });

        @SuppressWarnings("unused")
        WorldItem itemAfterMerge =
            targetWallet.merge(itemAfterFork)
                .foldRight(x -> {
                    targetWallet.add(x);
                    return x;
                })
                .thenLeft(result -> {
                    targetWallet.update(result);
                    return result;
                });
    }

    @Receive
    public void check(ExchangeReadyMessage msg) {
        TradeAction action = getTradeAction();
        action.trade.check(action.side);
        action.trade.conclude();
    }

    @Listener
    public InvitePlayerTradeEvent.AckT onInvited(InvitePlayerTradeEvent evt) {
        if (currentAction.isPresent()) {
            throw new InvitePlayerTradeEvent.Busy();
        }

        PlayerTrade trade = evt.getTrade();

        trade.getEventBus().subscribe(this);
        currentAction.set(new TradeAction(trade, WorldTradeSide.SECOND));

        client.write(new ExchangeRequestedTradeMessage(
                PLAYER_TRADE.value,
                evt.getSource().getId(),
                player.get().getId()
        ));

        return InvitePlayerTradeEvent.Ack;
    }

    @Listener
    public void onAcceptTrade(AcceptPlayerTradeEvent evt) {
        PlayerTrade trade = getTradeAction().trade;
        Player first = (Player) trade.getFirstTrader();
        Player second = (Player) trade.getSecondTrader();

        client.write(new ExchangeStartedWithPodsMessage(
                PLAYER_TRADE.value,
                first.getId(),
                first.getWallet().getWeight(),
                first.getMaxWeight(),
                second.getId(),
                second.getWallet().getWeight(),
                second.getMaxWeight()
        ));
    }

    @Listener
    public void onTradeEnd(PlayerTradeEndEvent evt) {
        evt.getTrade().getEventBus().unsubscribe(this);
        currentAction.remove();
        client.write(new ExchangeLeaveMessage(DialogTypeEnum.DIALOG_EXCHANGE.value, evt.getTrade().isConcluded()));
    }

    @Listener
    public void onTraderKamas(PlayerTraderKamasEvent evt) {
        client.write(new ExchangeKamaModifiedMessage(
            evt.getTrader() != player.get(),
            evt.getWallet().getKamas()));
    }

    @Listener
    public void onTraderAddItem(PlayerTraderAddItemEvent evt) {
        client.write(new ExchangeObjectAddedMessage(
            evt.getTrader() != player.get(),
            evt.getItem().toObjectItem()));
    }

    @Listener
    public void onTraderUpdateItem(PlayerTraderUpdateItemEvent evt) {
        client.write(new ExchangeObjectModifiedMessage(
            evt.getTrader() != player.get(),
            evt.getItem().toObjectItem()));
    }

    @Listener
    public void onTraderRemoveItem(PlayerTraderRemoveItemEvent evt) {
        client.write(new ExchangeObjectRemovedMessage(
            evt.getTrader() != player.get(),
            evt.getItem().getUid()));
    }

    @Listener
    public void onTradeCheck(PlayerTradeCheckEvent evt) {
        client.write(new ExchangeIsReadyMessage(evt.getTrader().getActorId(), evt.isCheck()));
    }

    @Listener
    public void onTradeConclude(PlayerTradeConcludeEvent evt) {
        TradeAction action = getTradeAction();
        Player player = this.player.get();
        PlayerItemWallet wallet = player.getWallet();

        WorldTrade.Result result = evt.getResult();
        WorldItemWallet lost = result.getBag(action.side);
        WorldItemWallet won = result.getBag(action.side.backwards());

        // add the won kamas minus the lost kamas
        wallet.plusKamas(won.getKamas() - lost.getKamas());

        // remove or decrease quantity of lost items
        lost.getItemStream()
            .forEach(item -> {
                if (item.getUid() < 0) {
                    WorldItem original = wallet.findByUid(-item.getUid()).get()
                        .plusQuantity(-item.getQuantity());

                    itemRepository.save(original)
                        .onSuccess(wallet::update);
                } else {
                    wallet.remove(item);
                }
            });

        // add or merge won items
        won.getItemStream()
            .forEach(item -> {
                if (item.getUid() < 0) {
                    wallet.merge(item)
                        .ifLeft(merged -> {
                            itemRepository.save(merged)
                                .onSuccess(wallet::update);
                        })
                        .ifRight(nonMerged -> {
                            wallet.add(item);
                        });
                } else {
                    wallet.add(item);
                }
            });

        client.transaction(tx -> {
            tx.write(new InventoryContentMessage(wallet.getItemStream().map(WorldItem::toObjectItem), wallet.getKamas()));
            tx.write(new InventoryWeightMessage(wallet.getWeight(), player.getMaxWeight()));
        });
    }
}
