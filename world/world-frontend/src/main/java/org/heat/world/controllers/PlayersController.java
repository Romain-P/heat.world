package org.heat.world.controllers;

import com.ankamagames.dofus.network.enums.CharacterCreationResultEnum;
import com.ankamagames.dofus.network.enums.GameContextEnum;
import com.ankamagames.dofus.network.messages.game.character.choice.*;
import com.ankamagames.dofus.network.messages.game.character.creation.CharacterCreationRequestMessage;
import com.ankamagames.dofus.network.messages.game.character.creation.CharacterCreationResultMessage;
import com.ankamagames.dofus.network.messages.game.character.creation.CharacterNameSuggestionRequestMessage;
import com.ankamagames.dofus.network.messages.game.character.creation.CharacterNameSuggestionSuccessMessage;
import com.ankamagames.dofus.network.messages.game.character.deletion.CharacterDeletionErrorMessage;
import com.ankamagames.dofus.network.messages.game.character.deletion.CharacterDeletionRequestMessage;
import com.ankamagames.dofus.network.messages.game.character.stats.CharacterStatsListMessage;
import com.ankamagames.dofus.network.messages.game.context.notification.NotificationListMessage;
import com.ankamagames.dofus.network.messages.game.context.roleplay.spell.SpellUpgradeFailureMessage;
import com.ankamagames.dofus.network.messages.game.context.roleplay.spell.SpellUpgradeRequestMessage;
import com.ankamagames.dofus.network.messages.game.context.roleplay.spell.SpellUpgradeSuccessMessage;
import com.ankamagames.dofus.network.messages.game.context.roleplay.stats.StatsUpgradeRequestMessage;
import com.ankamagames.dofus.network.messages.game.context.roleplay.stats.StatsUpgradeResultMessage;
import com.ankamagames.dofus.network.messages.game.initialization.CharacterLoadingCompleteMessage;
import com.ankamagames.dofus.network.messages.game.inventory.items.InventoryContentMessage;
import com.ankamagames.dofus.network.messages.game.inventory.items.InventoryWeightMessage;
import com.ankamagames.dofus.network.messages.game.inventory.spells.SpellListMessage;
import com.github.blackrush.acara.Listener;
import lombok.extern.slf4j.Slf4j;
import org.fungsi.Either;
import org.fungsi.Unit;
import org.fungsi.concurrent.Future;
import org.heat.User;
import org.heat.shared.Strings;
import org.heat.shared.function.UnsafeFunctions;
import org.heat.shared.stream.MoreCollectors;
import org.heat.world.backend.Backend;
import org.heat.world.controllers.events.*;
import org.heat.world.controllers.events.roleplay.EquipItemEvent;
import org.heat.world.controllers.utils.Authenticated;
import org.heat.world.controllers.utils.Idling;
import org.heat.world.items.WorldItem;
import org.heat.world.metrics.GameStats;
import org.heat.world.metrics.RegularStat;
import org.heat.world.players.*;
import org.heat.world.players.metrics.PlayerSpell;
import org.heat.world.players.metrics.PlayerStatBook;
import org.rocket.InjectConfig;
import org.rocket.network.*;

import javax.inject.Inject;
import javax.inject.Named;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

import static com.ankamagames.dofus.network.enums.CharacterCreationResultEnum.ERR_NO_REASON;
import static com.ankamagames.dofus.network.enums.CharacterCreationResultEnum.OK;
import static com.ankamagames.dofus.network.enums.CharacterDeletionErrorEnum.DEL_ERR_BAD_SECRET_ANSWER;

@Controller
@Authenticated
@PropValidation(value = Player.class, present = false)
@Slf4j
public class PlayersController {
    @Inject NetworkClient client;
    @Inject Prop<User> user;
    @Inject MutProp<Player> player;

    @Inject PlayerRepository players;
    @Inject PlayerFactory playerFactory;
    @Inject PlayerRegistry playerRegistry;
    @Inject Backend backend;
    @Inject @Named("pseudo") Random randomPseudo;

    @InjectConfig("heat.world.player.remove-required-answer-min-level")
    int removeRequiredAnswerMinLevel;

    List<Player> cached;

    List<Player> getPlayers() {
        if (cached == null) {
            // TODO(world/frontend): player load timeout
            cached = players.findByUserId(user.get().getId()).get();
            cached = new ArrayList<>(cached);
            Collections.sort(cached, Comparator.comparing(Player::getLastUsedAt).reversed());
        }
        return cached;
    }

    Either<Player, Unit> findPlayer(int id) {
        return getPlayers().stream().filter(x -> x.getId() == id).collect(MoreCollectors.uniqueMaybe());
    }

    Future<Unit> doChoose(Player player) {
        return client.getEventBus().publish(new ChoosePlayerEvent(player)).toUnit()
                .flatMap(u -> players.save(player))
                .flatMap(x -> {
                    this.player.set(player);
                    return client.transaction(tx -> {
                        tx.write(new NotificationListMessage(new int[0])); // TODO(world/players): notifications
                        tx.write(new CharacterSelectedSuccessMessage(player.toCharacterBaseInformations()));
                    });
                })
                .flatMap(x -> client.getEventBus().publish(new NewContextEvent(GameContextEnum.ROLE_PLAY)).toUnit())
                .flatMap(x -> client.write(CharacterLoadingCompleteMessage.i))
                ;
    }

    Future<Unit> writePlayerList() {
        return client.write(new CharactersListMessage(
                getPlayers().stream().map(Player::toCharacterBaseInformations),
                false // TODO(world/players): has startup actions
        ));
    }

    @Receive
    public void list(CharactersListRequestMessage msg) {
        writePlayerList();
    }

    @Receive
    public void create(CharacterCreationRequestMessage msg) {
        // create player
        playerFactory.create(user.get(), msg.name, msg.breed, msg.sex, msg.colors, msg.cosmeticId)
        // persist it to database
        .flatMap(player -> players.create(player).map(x -> player))
        // publish it
        .flatMap(player -> client.getEventBus().publish(new CreatePlayerEvent(player)).map(x -> player))
        // notify it to backend
        .onSuccess(player -> backend.setNrPlayers(user.get().getId(), getPlayers().size() + 1))
        // notify it to client
        .flatMap(player -> client.write(new CharacterCreationResultMessage(OK.value))
                .flatMap(x -> doChoose(player)))
        .onFailure(err -> log.error("cannot create player", err))
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

    private Future<Unit> remove0(Player player) {
        return players.remove(player)
                .flatMap(u -> {
                    cached.remove(player);
                    return writePlayerList();
                });
    }

    public static MessageDigest md5 = UnsafeFunctions.apply( () -> MessageDigest.getInstance("MD5") ).left(); // fucking checked exceptions

    public static String hash(int playerId, String secretAnswer) {
        String concat = Integer.toString(playerId, 10) + "~" + secretAnswer;
        byte[] digest = md5.digest(concat.getBytes());
        return Strings.toHexBytes(digest);
    }

    @Receive
    public void remove(CharacterDeletionRequestMessage msg) {
        Player player = findPlayer(msg.characterId).left();

        if (player.getExperience().getCurrentLevel() < removeRequiredAnswerMinLevel) {
            remove0(player);
            return;
        }

        String hash = hash(player.getId(), user.get().getSecretAnswer());

        if (!msg.secretAnswerHash.equalsIgnoreCase(hash)) {
            client.write(new CharacterDeletionErrorMessage(DEL_ERR_BAD_SECRET_ANSWER.value));
            return;
        }

        // TODO DEL_ERR_TOO_MANY_CHAR_DELETION

        remove0(player);
    }

    @Receive
    public void suggestName(CharacterNameSuggestionRequestMessage msg) {
        // TODO(world/players): enable or disable pseudo suggestion
        client.write(new CharacterNameSuggestionSuccessMessage(Strings.randomPseudo(randomPseudo)));
    }

    @Receive
    public void choose(CharacterSelectionMessage msg) {
        findPlayer(msg.id)
            .foldLeft(this::doChoose)
            .thenRight(x -> client.write(CharacterSelectedErrorMessage.i))
            .onFailure(err -> log.error("cannot choose player", err))
            ;
    }

    @Listener
    public void setLastUsedAt(ChoosePlayerEvent evt) {
        evt.getPlayer().setLastUsedAt(Instant.now());
    }

    @Listener
    public void createRolePlayContext(CreateContextEvent evt) {
        if (evt.getContext() != GameContextEnum.ROLE_PLAY) return;

        Player player = this.player.get();

        client.transaction(tx -> {
            tx.write(new SpellListMessage(false, player.getSpells().toSpellItem()));

            tx.write(new CharacterStatsListMessage(player.toCharacterCharacteristicsInformations()));

            tx.write(new InventoryContentMessage(
                    player.getWallet().getItemStream().map(WorldItem::toObjectItem),
                    player.getWallet().getKamas()
            ));
            tx.write(new InventoryWeightMessage(
                    player.getWallet().getWeight(),
                    player.getMaxWeight()
            ));
        });
    }

    @Listener
    public void appearHimselfOnline(EnterContextEvent evt) {
        playerRegistry.add(player.get());
    }

    @Listener
    public void appearHimselfOffline(DestroyContextEvent evt) {
        playerRegistry.remove(player.get());
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

    @Receive
    @Idling
    public void upgradeSpell(SpellUpgradeRequestMessage msg) {
        Player player = this.player.get();
        PlayerSpell spell = player.getSpells().findById(msg.spellId).get();

        int cost = Players.getCostUpgradeSpell(spell.getLevel(), msg.spellLevel);

        if (cost > player.getStats().getSpellsPoints()) {
            client.write(SpellUpgradeFailureMessage.i);
        } else if (player.getExperience().getCurrentLevel() < spell.getMinPlayerLevel()) {
            client.write(SpellUpgradeFailureMessage.i);
        } else {
            player.getStats().plusSpellsPoints(-cost);
            spell.setLevel(msg.spellLevel);
            players.save(player);

            client.transaction(tx -> {
                tx.write(new SpellUpgradeSuccessMessage(spell.getId(), spell.getLevel()));
                tx.write(new CharacterStatsListMessage(player.toCharacterCharacteristicsInformations()));
            });
        }
    }

    @Listener
    public void applyItemToStatBook(EquipItemEvent evt) {
        Player player = this.player.get();
        PlayerStatBook stats = player.getStats();
        if (evt.isApply()) {
            stats.apply(evt.getItem());
        } else {
            stats.unapply(evt.getItem());
        }

        client.write(new CharacterStatsListMessage(player.toCharacterCharacteristicsInformations()));
    }
}
