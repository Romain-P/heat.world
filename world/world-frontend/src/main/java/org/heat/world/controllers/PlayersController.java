package org.heat.world.controllers;

import com.ankamagames.dofus.network.enums.CharacterCreationResultEnum;
import com.ankamagames.dofus.network.enums.GameContextEnum;
import com.ankamagames.dofus.network.messages.game.character.choice.*;
import com.ankamagames.dofus.network.messages.game.character.creation.CharacterCreationRequestMessage;
import com.ankamagames.dofus.network.messages.game.character.creation.CharacterCreationResultMessage;
import com.ankamagames.dofus.network.messages.game.character.creation.CharacterNameSuggestionRequestMessage;
import com.ankamagames.dofus.network.messages.game.character.creation.CharacterNameSuggestionSuccessMessage;
import com.ankamagames.dofus.network.messages.game.character.stats.CharacterStatsListMessage;
import com.ankamagames.dofus.network.messages.game.context.notification.NotificationListMessage;
import com.ankamagames.dofus.network.messages.game.context.roleplay.stats.StatsUpgradeRequestMessage;
import com.ankamagames.dofus.network.messages.game.context.roleplay.stats.StatsUpgradeResultMessage;
import com.ankamagames.dofus.network.messages.game.initialization.CharacterLoadingCompleteMessage;
import com.github.blackrush.acara.Listener;
import org.fungsi.Unit;
import org.fungsi.concurrent.Future;
import org.heat.User;
import org.heat.shared.Strings;
import org.heat.shared.stream.MoreCollectors;
import org.heat.world.backend.Backend;
import org.heat.world.controllers.events.CreateContextEvent;
import org.heat.world.controllers.events.NewContextEvent;
import org.heat.world.controllers.utils.Authenticated;
import org.heat.world.controllers.utils.Idling;
import org.heat.world.metrics.GameStats;
import org.heat.world.metrics.RegularStat;
import org.heat.world.players.Player;
import org.heat.world.players.PlayerCreationException;
import org.heat.world.players.PlayerFactory;
import org.heat.world.players.PlayerRepository;
import org.rocket.network.*;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.Random;

import static com.ankamagames.dofus.network.enums.CharacterCreationResultEnum.ERR_NO_REASON;
import static com.ankamagames.dofus.network.enums.CharacterCreationResultEnum.OK;

@Controller
@Authenticated
@PropValidation(value = Player.class, present = false)
public class PlayersController {
    @Inject NetworkClient client;
    @Inject Prop<User> user;
    @Inject MutProp<Player> player;

    @Inject PlayerRepository players;
    @Inject PlayerFactory playerFactory;
    @Inject Backend backend;
    @Inject @Named("pseudo") Random randomPseudo;

    List<Player> cached;

    List<Player> getPlayers() {
        if (cached == null) {
            cached = players.findByUserId(user.get().getId());
        }
        return cached;
    }

    Future<Unit> doChoose(Player player) {
        this.player.set(player);
        return client.transaction(tx -> {
                    tx.write(new NotificationListMessage(new int[0])); // TODO(world/players): notifications
                    tx.write(new CharacterSelectedSuccessMessage(player.toCharacterBaseInformations()));
                })
                .flatMap(x -> client.getEventBus().publish(new NewContextEvent(GameContextEnum.ROLE_PLAY)).toUnit())
                .flatMap(x -> client.write(CharacterLoadingCompleteMessage.i))
                ;
    }

    @Receive
    public void list(CharactersListRequestMessage msg) {
        client.write(new CharactersListMessage(
                getPlayers().stream().map(Player::toCharacterBaseInformations),
                false // TODO(world/players): has startup actions
        ));
    }

    @Receive
    public void create(CharacterCreationRequestMessage msg) {
        playerFactory.create(user.get(), msg.name, msg.breed, msg.sex, msg.colors, msg.cosmeticId)
                .onSuccess(player -> {
                    players.save(player);
                    backend.setNrPlayers(user.get().getId(), getPlayers().size() + 1);
                })
                .flatMap(player -> client.write(new CharacterCreationResultMessage(OK.value))
                        .flatMap(x -> doChoose(player)))
                .mayRescue(cause -> {
                    CharacterCreationResultEnum reason;
                    if (cause instanceof PlayerCreationException) {
                        reason = ((PlayerCreationException) cause).getReason();
                    } else {
                        reason = ERR_NO_REASON;
                    }
                    return client.write(new CharacterCreationResultMessage(reason.value));
                })
        ;
    }

    @Receive
    public void suggestName(CharacterNameSuggestionRequestMessage msg) {
        // TODO(world/players): enable or disable pseudo suggestion
        client.write(new CharacterNameSuggestionSuccessMessage(Strings.randomPseudo(randomPseudo)));
    }

    @Receive
    public void choose(CharacterSelectionMessage msg) {
        getPlayers().stream().filter(x -> x.getId() == msg.id).collect(MoreCollectors.uniqueMaybe())
                .ifLeft(this::doChoose)
                .ifRight(x -> client.write(CharacterSelectedErrorMessage.i))
        ;
    }

    @Listener
    public void createRolePlayContext(CreateContextEvent evt) {
        if (evt.getContext() != GameContextEnum.ROLE_PLAY) return;

        client.write(new CharacterStatsListMessage(player.get().toCharacterCharacteristicsInformations()));
    }

    @Receive
    @Idling
    public void upgradeStat(StatsUpgradeRequestMessage msg) {
        Player player = this.player.get();
        GameStats<RegularStat> stat = GameStats.findBoostable(msg.statId).get();
        short upgraded = (short) player.getStats().upgradeStat(stat, msg.boostPoint);

        players.save(player);

        client.transaction(tx -> {
            tx.write(new StatsUpgradeResultMessage(upgraded));
            tx.write(new CharacterStatsListMessage(player.toCharacterCharacteristicsInformations()));
        });
    }
}
