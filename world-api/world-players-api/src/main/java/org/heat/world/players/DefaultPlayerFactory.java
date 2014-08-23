package org.heat.world.players;

import com.ankamagames.dofus.datacenter.breeds.Breed;
import com.ankamagames.dofus.datacenter.breeds.Head;
import com.ankamagames.dofus.network.enums.DirectionsEnum;
import com.github.blackrush.acara.EventBusBuilder;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.typesafe.config.Config;
import org.fungsi.concurrent.Future;
import org.fungsi.concurrent.Futures;
import org.heat.User;
import org.heat.data.Datacenter;
import org.heat.world.items.MapItemBag;
import org.heat.world.metrics.Experience;
import org.heat.world.metrics.GameStats;
import org.heat.world.players.items.LazyPlayerItemWallet;
import org.heat.world.players.items.PlayerItemRepository;
import org.heat.world.players.items.PlayerItemWallet;
import org.heat.world.players.metrics.*;
import org.heat.world.players.shortcuts.LazyShortcutBar;
import org.heat.world.players.shortcuts.PlayerShortcutBar;
import org.heat.world.players.shortcuts.PlayerShortcutRepository;
import org.heat.world.roleplay.WorldActorLook;
import org.heat.world.roleplay.environment.WorldMapPoint;
import org.heat.world.roleplay.environment.WorldPosition;
import org.heat.world.roleplay.environment.WorldPositioningSystem;
import org.rocket.Service;
import org.rocket.ServiceContext;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Singleton
public class DefaultPlayerFactory implements PlayerFactory, Service {
    private final Datacenter datacenter;
    private final AtomicInteger idGenerator;
    private final PlayerItemRepository playerItems;
    private final PlayerShortcutRepository playerShortcuts;
    private final EventBusBuilder eventBusBuilder;

    // these properties are immutable therefore freely sharable
    private WorldPosition startPosition;
    private PlayerExperience startExperience;
    private short
            startLife,
            maxEnergy,
            startEnergy,
            startActions,
            startMovements,
            startProspecting,
            startStatsPoints,
            startSpellsPoints;
    private int
            startKamas
            ;

    @Inject
    public DefaultPlayerFactory(
            Datacenter datacenter,
            PlayerRepository players,
            PlayerItemRepository playerItems,
            PlayerShortcutRepository playerShortcuts,
            @Named("player") EventBusBuilder eventBusBuilder
    ) {
        this.datacenter = datacenter;
        this.idGenerator = players.createIdGenerator().get();
        this.playerItems = playerItems;
        this.playerShortcuts = playerShortcuts;
        this.eventBusBuilder = eventBusBuilder;
    }

    @Override
    public Optional<Class<? extends Service>> dependsOn() {
        return Optional.of(WorldPositioningSystem.class);
    }

    @Override
    public void start(ServiceContext ctx) {
        Config startConfig = ctx.getConfig().getConfig("heat.world.player.start");

        WorldPositioningSystem wps = ctx.getInjector().getInstance(WorldPositioningSystem.class);
        Experience experience = ctx.getInjector().getInstance(Key.get(Experience.class, Names.named("player")));

        this.startPosition = wps.locate(
                startConfig.getInt("map"),
                WorldMapPoint.of(startConfig.getInt("cell")).get(),
                DirectionsEnum.valueOf((byte) startConfig.getInt("direction")).get()
        );

        Experience playerExperience = experience.getNextUntilIsLevel(startConfig.getInt("level"));
        this.startExperience = new PlayerExperience(playerExperience.getTop(), playerExperience);

        this.startLife         = (short) startConfig.getInt("life");
        this.maxEnergy         = (short) startConfig.getInt("max-energy");
        this.startEnergy       = (short) startConfig.getInt("energy");
        this.startActions      = (short) startConfig.getInt("actions");
        this.startMovements    = (short) startConfig.getInt("movements");
        this.startProspecting  = (short) startConfig.getInt("prospecting");
        this.startStatsPoints  = (short) startConfig.getInt("stats-points");
        this.startSpellsPoints = (short) startConfig.getInt("spells-points");
        this.startKamas        =         startConfig.getInt("kamas");
    }

    @Override
    public void stop(ServiceContext ctx) { }

    @Override
    public Future<Player> create(User user, String name, byte breed, boolean sex, int[] colors, int cosmeticId) {
        Player player = new Player();
        player.setEventBus(eventBusBuilder.build());
        player.setId(idGenerator.incrementAndGet());
        player.setUserId(user.getId());
        player.setName(name);
        player.setBreed(datacenter.find(Breed.class, breed).get());
        player.setSex(sex);
        player.setLook(buildLook(player.getBreed(), sex, colors, cosmeticId));
        player.setPosition(buildPosition(player.getBreed()));
        player.setExperience(buildExperience());
        player.setStats(buildStats(player.getBreed()));
        player.setSpells(buildSpells(player.getBreed(), player.getExperience().getCurrentLevel()));
        player.setWallet(buildWallet(player.getId()));
        player.setShortcutBar(buildShortcutBar(player.getId()));
        player.setLastUsedAt(Instant.now());
        return Futures.success(player);
    }

    protected WorldActorLook buildLook(Breed breed, boolean sex, int[] colors, int cosmeticId) {
        long[] defaultColors = sex ? breed.getFemaleColors() : breed.getMaleColors();
        for (int i = 0; i < colors.length; i++) {
            if (colors[i] == -1) {
                colors[i] = (int) defaultColors[i];
            }
        }


        String look = sex ? breed.getFemaleLook() : breed.getMaleLook();
        // {1|10||135}
        //  -^--^ ---
        //   A  B
        int A = look.indexOf('|');
        int B = look.indexOf('|', A + 1);

        short lookId = Short.parseShort(look.substring(A + 1, B));
        short scale = Short.parseShort(look.substring(B + 2, look.length() - 1));



        Head head = datacenter.find(Head.class, cosmeticId).get();
        short headId = Short.parseShort(head.getSkins());


        return new WorldActorLook(WorldActorLook.Bones.STANDING, lookId, headId, scale, WorldActorLook.toIndexedColors(colors));
    }

    @SuppressWarnings("UnusedParameters")
    protected WorldPosition buildPosition(Breed breed) {
        return startPosition;
    }

    protected PlayerExperience buildExperience() {
        return startExperience;
    }

    protected PlayerStatBook buildStats(Breed breed) {
        DefaultPlayerStatBook book = new DefaultPlayerStatBook(breed);

        book.get(GameStats.LIFE).setCurrentAndMax(startLife);
        book.get(GameStats.ENERGY).setCurrentAndMax(startEnergy, maxEnergy);
        book.get(GameStats.ACTIONS).setBase(startActions);
        book.get(GameStats.MOVEMENTS).setBase(startMovements);
        book.get(GameStats.PROSPECTING).setBase(startProspecting);
        book.get(GameStats.SUMMONABLE_CREATURES).setBase((short) 1);

        book.setStatsPoints(startStatsPoints);
        book.setSpellsPoints(startSpellsPoints);

        return book;
    }

    protected PlayerSpellBook buildSpells(Breed breed, int level) {
        return DefaultPlayerSpellBook.create(datacenter, breed, level);
    }

    protected PlayerItemWallet buildWallet(int playerId) {
        return new LazyPlayerItemWallet(startKamas, playerId, playerItems, MapItemBag::newHashMapItemBag);
    }

    protected PlayerShortcutBar buildShortcutBar(int playerId) {
        return new LazyShortcutBar(playerShortcuts, playerId);
    }
}
