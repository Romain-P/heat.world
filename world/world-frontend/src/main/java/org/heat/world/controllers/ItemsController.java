package org.heat.world.controllers;

import com.ankamagames.dofus.network.enums.CharacterInventoryPositionEnum;
import com.ankamagames.dofus.network.enums.ObjectErrorEnum;
import com.ankamagames.dofus.network.messages.game.basic.BasicNoOperationMessage;
import com.ankamagames.dofus.network.messages.game.character.stats.CharacterStatsListMessage;
import com.ankamagames.dofus.network.messages.game.inventory.items.*;
import com.github.blackrush.acara.Listener;
import org.fungsi.Either;
import org.heat.shared.MoreFutures;
import org.heat.shared.Pair;
import org.heat.world.controllers.events.CreatePlayerEvent;
import org.heat.world.controllers.utils.Idling;
import org.heat.world.items.WorldItem;
import org.heat.world.items.WorldItemFactory;
import org.heat.world.items.WorldItemRepository;
import org.heat.world.players.Player;
import org.heat.world.players.items.PlayerItemWallet;
import org.heat.world.roleplay.environment.WorldMap;
import org.heat.world.roleplay.environment.WorldMapPoint;
import org.rocket.network.Controller;
import org.rocket.network.NetworkClient;
import org.rocket.network.Prop;
import org.rocket.network.Receive;

import javax.inject.Inject;

import static com.ankamagames.dofus.network.enums.ObjectErrorEnum.CANNOT_DROP_NO_PLACE;

@Controller
@Idling
public class ItemsController {
    @Inject NetworkClient client;
    @Inject Prop<Player> player;

    @Inject WorldItemRepository items;
    @Inject WorldItemFactory itemFactory;


    @Listener
    public void onPlayerCreation(CreatePlayerEvent evt) {
        // DEBUG(world/frontend)
        items.save(itemFactory.create(39, 1)) // small owl amulet
                .onSuccess(evt.getPlayer().getWallet()::add)
                ;

        items.save(itemFactory.create(100, 2)) // small wisdom ring
                .onSuccess(evt.getPlayer().getWallet()::add)
                ;
    }

    @Receive
    public void moveItem(ObjectSetPositionMessage msg) {
        Player player = this.player.get();
        PlayerItemWallet wallet = player.getWallet();

        CharacterInventoryPositionEnum position = CharacterInventoryPositionEnum.valueOf((byte) msg.position).get();
        int quantity = msg.quantity;

        // get item
        WorldItem item = wallet.findByUid(msg.objectUID).get();

        // verify movement validity
        if (!player.canMoveItemTo(item, position, quantity)) {
            client.write(new ObjectErrorMessage(ObjectErrorEnum.CANNOT_EQUIP_HERE.value));
            return;
        }

        // fork!
        Either<Pair<WorldItem, WorldItem>, WorldItem> fork = wallet.fork(item, quantity);
        if (fork.isLeft()) {
            // forked...
            Pair<WorldItem, WorldItem> forkPair = fork.left();
            WorldItem original = forkPair.first;
            WorldItem forked = forkPair.second;

            // merge or move!
            Either<WorldItem, WorldItem> mergeOrMove = wallet.mergeOrMove(forked, position);
            if (mergeOrMove.isLeft()) {
                // merged...
                WorldItem merged = mergeOrMove.left();

                MoreFutures.join(items.save(original), items.save(merged))
                        .onSuccess(pair -> {
                            wallet.update(pair.first);
                            wallet.update(pair.second);

                            client.transaction(tx -> {
                                tx.write(new ObjectQuantityMessage(pair.first.getUid(), pair.first.getQuantity()));
                                tx.write(new ObjectQuantityMessage(pair.second.getUid(), pair.second.getQuantity()));
                                tx.write(new InventoryWeightMessage(player.getWallet().getWeight(), player.getStats().getMaxWeight()));
                                tx.write(new CharacterStatsListMessage(player.toCharacterCharacteristicsInformations()));
                                tx.write(BasicNoOperationMessage.i);
                            });
                        });
            } else {
                // moved...
                WorldItem moved = mergeOrMove.right();

                MoreFutures.join(items.save(original), items.save(moved))
                        .onSuccess(pair -> {
                            wallet.update(pair.first);
                            wallet.add(pair.second);

                            client.transaction(tx -> {
                                tx.write(new ObjectQuantityMessage(pair.first.getUid(), pair.first.getQuantity()));
                                tx.write(new ObjectAddedMessage(pair.second.toObjectItem()));
                                tx.write(new InventoryWeightMessage(player.getWallet().getWeight(), player.getStats().getMaxWeight()));
                                tx.write(new CharacterStatsListMessage(player.toCharacterCharacteristicsInformations()));
                                tx.write(BasicNoOperationMessage.i);
                            });
                        });
            }
        } else {
            // not forked...

            // merge or move!
            Either<WorldItem, WorldItem> mergeOrMove = wallet.mergeOrMove(item, position);
            if (mergeOrMove.isLeft()) {
                // merged...
                WorldItem merged = mergeOrMove.left();

                MoreFutures.join(items.remove(item), items.save(merged))
                        .onSuccess(pair -> {
                            wallet.remove(pair.first);
                            wallet.update(pair.second);

                            client.transaction(tx -> {
                                tx.write(new ObjectDeletedMessage(pair.first.getUid()));
                                tx.write(new ObjectQuantityMessage(pair.second.getUid(), pair.second.getQuantity()));
                                tx.write(new InventoryWeightMessage(player.getWallet().getWeight(), player.getStats().getMaxWeight()));
                                tx.write(new CharacterStatsListMessage(player.toCharacterCharacteristicsInformations()));
                                tx.write(BasicNoOperationMessage.i);
                            });
                        });
            } else {
                // moved...
                WorldItem moved = mergeOrMove.right();

                items.save(moved)
                        .onSuccess(x -> {
                            wallet.update(x);
                            client.transaction(tx -> {
                                tx.write(new ObjectMovementMessage(x.getUid(), x.getPosition().value));
                                tx.write(new InventoryWeightMessage(player.getWallet().getWeight(), player.getStats().getMaxWeight()));
                                tx.write(new CharacterStatsListMessage(player.toCharacterCharacteristicsInformations()));
                                tx.write(BasicNoOperationMessage.i);
                            });
                        });
            }
        }
    }

    @Receive
    public void dropItem(ObjectDropMessage msg) {
        Player player = this.player.get();
        PlayerItemWallet wallet = player.getWallet();
        WorldMap map = player.getPosition().getMap();
        WorldMapPoint mapPoint = player.getPosition().getMapPoint();

        WorldItem item = wallet.findByUid(msg.objectUID).get();
        int quantity = msg.quantity;

        Either<Pair<WorldItem, WorldItem>, WorldItem> fork = wallet.fork(item, quantity);
        if (fork.isLeft()) {
            Pair<WorldItem, WorldItem> forkPair = fork.left();
            WorldItem original = forkPair.first;
            WorldItem forked = forkPair.second;

            if (!map.addItem(forked, mapPoint, false)) {
                client.write(new ObjectErrorMessage(CANNOT_DROP_NO_PLACE.value));
                return;
            }

            MoreFutures.join(items.save(original), items.save(forked))
                .onSuccess(pair -> {
                    wallet.update(pair.first);

                    client.transaction(tx -> {
                        client.write(new ObjectQuantityMessage(pair.first.getUid(), pair.first.getQuantity()));
                        tx.write(new InventoryWeightMessage(wallet.getWeight(), player.getStats().getMaxWeight()));
                        tx.write(new CharacterStatsListMessage(player.toCharacterCharacteristicsInformations()));
                        tx.write(BasicNoOperationMessage.i);
                    });
                });
        } else {
            if (!map.addItem(item, mapPoint, false)) {
                client.write(new ObjectErrorMessage(CANNOT_DROP_NO_PLACE.value));
                return;
            }

            wallet.remove(item);

            client.transaction(tx -> {
                tx.write(new ObjectDeletedMessage(item.getUid()));
                tx.write(new InventoryWeightMessage(wallet.getWeight(), player.getStats().getMaxWeight()));
                tx.write(new CharacterStatsListMessage(player.toCharacterCharacteristicsInformations()));
                tx.write(BasicNoOperationMessage.i);
            });
        }
    }
}
