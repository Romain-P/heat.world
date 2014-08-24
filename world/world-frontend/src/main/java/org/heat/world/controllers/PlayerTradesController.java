package org.heat.world.controllers;

import com.ankamagames.dofus.network.enums.DialogTypeEnum;
import com.ankamagames.dofus.network.enums.GameContextEnum;
import com.ankamagames.dofus.network.messages.game.dialog.LeaveDialogRequestMessage;
import com.ankamagames.dofus.network.messages.game.inventory.exchanges.*;
import com.ankamagames.dofus.network.messages.game.inventory.items.ExchangeKamaModifiedMessage;
import com.github.blackrush.acara.Listener;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.fungsi.concurrent.Future;
import org.fungsi.concurrent.Promise;
import org.fungsi.concurrent.Promises;
import org.heat.world.controllers.events.EnterContextEvent;
import org.heat.world.controllers.events.roleplay.trades.AcceptPlayerTradeEvent;
import org.heat.world.controllers.events.roleplay.trades.InvitePlayerTradeEvent;
import org.heat.world.items.WorldItemWallet;
import org.heat.world.players.Player;
import org.heat.world.roleplay.WorldAction;
import org.heat.world.roleplay.environment.WorldMap;
import org.heat.world.trading.WorldTradeSide;
import org.heat.world.trading.impl.player.PlayerTrade;
import org.heat.world.trading.impl.player.PlayerTradeFactory;
import org.heat.world.trading.impl.player.events.*;
import org.rocket.network.*;

import javax.inject.Inject;
import java.util.Optional;

import static com.ankamagames.dofus.network.enums.ExchangeErrorEnum.REQUEST_IMPOSSIBLE;
import static com.ankamagames.dofus.network.enums.ExchangeTypeEnum.PLAYER_TRADE;

@Controller
public class PlayerTradesController {
    @Inject NetworkClient client;
    @Inject Prop<Player> player;
    @Inject MutProp<WorldAction> currentAction;

    @Inject PlayerTradeFactory tradeFactory;

    @RequiredArgsConstructor
    @Getter
    class TradeAction implements WorldAction {
        final PlayerTrade trade;
        final WorldTradeSide side;
        final Player actor = player.get();
        final WorldItemWallet wallet = actor.getWallet().createTemp();
        final Promise<WorldAction> endFuture = Promises.create();

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
        action.trade.getEventBus().publish(AcceptPlayerTradeEvent.INSTANCE);
    }

    @Receive
    public void setKamas(ExchangeObjectMoveKamaMessage msg) {
        TradeAction action = getTradeAction();

        if (action.wallet.getKamas() < msg.quantity) {
            // todo send error
            return;
        }

        action.wallet.plusKamas(-msg.quantity);
        action.trade.addKamas(action.side, msg.quantity);
    }

    @Receive
    public void setItem(ExchangeObjectMoveMessage msg) {

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
    public void onCancelTrade(PlayerTradeCancelEvent evt) {
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

    }

    @Listener
    public void onTraderRemoveItem(PlayerTraderRemoveItemEvent evt) {

    }

    @Listener
    public void onTradeCheck(PlayerTradeCheckEvent evt) {

    }
}
