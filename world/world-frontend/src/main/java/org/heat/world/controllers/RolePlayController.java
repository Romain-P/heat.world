package org.heat.world.controllers;

import com.ankamagames.dofus.network.messages.game.character.stats.LifePointsRegenBeginMessage;
import com.ankamagames.dofus.network.messages.game.context.*;
import com.ankamagames.dofus.network.messages.game.context.roleplay.*;
import com.github.blackrush.acara.Listener;
import lombok.extern.slf4j.Slf4j;
import org.heat.world.controllers.events.CreateContextEvent;
import org.heat.world.controllers.events.DestroyContextEvent;
import org.heat.world.controllers.events.EnterContextEvent;
import org.heat.world.controllers.events.QuitContextEvent;
import org.heat.world.controllers.utils.Basics;
import org.heat.world.controllers.utils.Idling;
import org.heat.world.controllers.utils.RolePlaying;
import org.heat.world.players.Player;
import org.heat.world.roleplay.WorldAction;
import org.heat.world.roleplay.WorldActor;
import org.heat.world.roleplay.environment.*;
import org.heat.world.roleplay.environment.events.ActorEntranceEvent;
import org.heat.world.roleplay.environment.events.ActorMovementEvent;
import org.heat.world.roleplay.environment.events.ActorRefreshEvent;
import org.rocket.network.*;

import javax.inject.Inject;
import java.util.stream.Stream;

import static com.ankamagames.dofus.network.enums.GameContextEnum.ROLE_PLAY;

@Controller
@RolePlaying
@Slf4j
public class RolePlayController {
    @Inject NetworkClient client;

    @Inject Prop<Player> player;
    @Inject MutProp<WorldAction> currentAction;

    @Receive
    public void createContext(GameContextCreateRequestMessage msg) {
        client.transaction(tx -> {
            tx.write(new GameContextDestroyMessage());
            tx.write(new GameContextCreateMessage(ROLE_PLAY.value));

            // TODO(world/frontend): life points regen
            tx.write(new LifePointsRegenBeginMessage((short) 1));
        });

        client.getEventBus().publish(new CreateContextEvent(ROLE_PLAY));
    }

    @Disconnect
    public void destroyContext() {
        if (player.isPresent()) {
            client.getEventBus().publish(new QuitContextEvent(ROLE_PLAY));
            client.getEventBus().publish(new DestroyContextEvent(ROLE_PLAY));
        }
    }

    @Receive
    public void getMapInfos(MapInformationsRequestMessage msg) {
        WorldPosition position = player.get().getPosition();

        if (msg.mapId != position.getMapId()) {
            // NOTE(Blackrush): just log a warning for now
            log.warn("client {} requested wrong map informations", client);
        }

        // NOTE(Alleos13):
        //    Go in house?? => MapComplementaryInformationsDataInHouseMessage
        //    Or was in house => MapComplementaryInformationsWithCoordsMessage
        //    Else
        client.write(new MapComplementaryInformationsDataMessage(
                (short) position.getSubAreaId(),
                position.getMapId(),
                Stream.empty(), // TODO(world): houses
                position.getMap().getActorStream().map(WorldActor::toGameRolePlayActorInformations),
                Stream.empty(), // TODO(world): interactive elements
                Stream.empty(), // TODO(world): stated elements
                Stream.empty(), // TODO(world): map obstacles
                Stream.empty()  // TODO(world): fights
        ));

        client.getEventBus().publish(new EnterContextEvent(ROLE_PLAY));
    }

    @Receive
    public void changeMap(ChangeMapMessage msg) {
        Player player = this.player.get();
        WorldPosition newPos = player.getPosition().goToMap(msg.mapId);

        client.getEventBus().publish(new QuitContextEvent(ROLE_PLAY))
                .flatMap(x -> {
                    player.setPosition(newPos);
                    return client.getEventBus().publish(new CreateContextEvent(ROLE_PLAY));
                });
    }

    @Receive
    @Idling
    public void move(GameMapMovementRequestMessage msg) {
        Player player = this.player.get();

        WorldMapPath path = WorldMapPath.parse(player.getPosition(), msg.keyMovements);
        if (!path.isValid()) {
            throw new InvalidMapPathException();
        }

        currentAction.set(new WorldMovement(player, path));
        player.getPosition().getMap().moveActor(player, path);
    }

    @Receive
    public void moveConfirmation(GameMapMovementConfirmMessage msg) {
        WorldMovement movement = (WorldMovement) currentAction.get();
        currentAction.remove();

        Player player = this.player.get();
        WorldMapPath path = movement.getPath();
        player.moveTo(path.target().getPoint(), path.target().getDir());

        movement.notifyEnd();

        client.write(Basics.noop());
    }

    @Receive
    public void moveCancellation(GameMapMovementCancelMessage msg) {
        WorldMovement movement = (WorldMovement) currentAction.get();
        currentAction.remove();

        Player player = this.player.get();
        WorldMapPath path = movement.getPath();
        WorldMapPoint cancellationPoint = WorldMapPoint.of(msg.cellId).get();

        if (!path.contains(cancellationPoint)) {
            throw new IllegalArgumentException("you can not cancel your path here");
        }
        player.moveTo(cancellationPoint, player.getPosition().getDirection());

        movement.notifyCancellation(cancellationPoint);

        client.write(Basics.noop());
    }

    @Listener
    public void createRolePlayContext(CreateContextEvent evt) {
        Player player = this.player.get();

        client.transaction(tx -> {
            tx.write(new CurrentMapMessage(
                    player.getPosition().getMapId(),
                    player.getPosition().getMapData().getKey()
            ));

            tx.write(Basics.time());
            tx.write(Basics.noop());
        });
    }

    @Listener
    public void enterRolePlayContext(EnterContextEvent evt) {
        if (evt.getContext() != ROLE_PLAY) return;

        Player player = this.player.get();
        player.getPosition().getMap().getEventBus().subscribe(this);
        player.getPosition().getMap().addActor(player);
    }

    @Listener
    public void quitRolePlayContext(QuitContextEvent evt) {
        if (evt.getContext() != ROLE_PLAY) return;

        Player player = this.player.get();
        player.getPosition().getMap().removeActor(player);
        player.getPosition().getMap().getEventBus().unsubscribe(this);
    }

    @Listener
    public void actorEntranceOnMap(ActorEntranceEvent evt) {
        if (evt.isEntering()) {
            client.write(new GameRolePlayShowActorMessage(evt.getActor().toGameRolePlayActorInformations()));
        } else {
            client.write(new GameContextRemoveElementMessage(evt.getActor().getActorId()));
        }
    }

    @Listener
    public void actorRefreshOnMap(ActorRefreshEvent evt) {
        client.write(new GameContextRefreshEntityLookMessage(
                evt.getActor().getActorId(),
                evt.getActor().getActorLook().toEntityLook()
        ));
    }

    @Listener
    public void actorMovementOnMap(ActorMovementEvent evt) {
        client.write(new GameMapMovementMessage(evt.getPath().export(), evt.getActor().getActorId()));
    }
}
