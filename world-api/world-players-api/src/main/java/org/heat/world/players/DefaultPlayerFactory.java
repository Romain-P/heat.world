package org.heat.world.players;

import com.ankamagames.dofus.datacenter.breeds.Breed;
import com.ankamagames.dofus.datacenter.breeds.Head;
import com.ankamagames.dofus.network.enums.DirectionsEnum;
import com.typesafe.config.Config;
import org.fungsi.concurrent.Future;
import org.fungsi.concurrent.Futures;
import org.heat.User;
import org.heat.data.Datacenter;
import org.heat.world.metrics.Experience;
import org.heat.world.metrics.GameStats;
import org.heat.world.roleplay.WorldActorLook;
import org.heat.world.roleplay.environment.WorldMapPoint;
import org.heat.world.roleplay.environment.WorldPosition;
import org.heat.world.roleplay.environment.WorldPositioningSystem;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.concurrent.atomic.AtomicInteger;

public class DefaultPlayerFactory implements PlayerFactory {
    private final Datacenter datacenter;
    private final AtomicInteger idGenerator;

    // these properties are immutable therefore freely sharable
    private final WorldPosition startPosition;
    private final PlayerExperience startExperience;
    private final short
            startLife,
            maxEnergy,
            startEnergy,
            startActions,
            startMovements,
            startProspecting,
            startStatsPoints,
            startSpellsPoints;

    @Inject
    public DefaultPlayerFactory(
            Datacenter datacenter,
            PlayerRepository players,
            Config config,
            @Named("player") Experience experience,
            WorldPositioningSystem wps
    ) {
        this.datacenter = datacenter;
        this.idGenerator = players.createIdGenerator().get();

        Config startConfig = config.getConfig("heat.world.player.start");

        this.startPosition = wps.locate(
                startConfig.getInt("map"),
                WorldMapPoint.of(startConfig.getInt("cell")).get(),
                DirectionsEnum.valueOf((byte) startConfig.getInt("direction")).get()
        );

        Experience startStep = experience.getNextUntilIsLevel(startConfig.getInt("level"));
        this.startExperience = new PlayerExperience(startStep.getTop(), startStep);

        this.startLife         = (short) startConfig.getInt("life");
        this.maxEnergy         = (short) startConfig.getInt("max-energy");
        this.startEnergy       = (short) startConfig.getInt("energy");
        this.startActions      = (short) startConfig.getInt("actions");
        this.startMovements    = (short) startConfig.getInt("movements");
        this.startProspecting  = (short) startConfig.getInt("prospecting");
        this.startStatsPoints  = (short) startConfig.getInt("stats-points");
        this.startSpellsPoints = (short) startConfig.getInt("spells-points");
    }

    @Override
    public Future<Player> create(User user, String name, byte breed, boolean sex, int[] colors, int cosmeticId) {
        Player player = new Player();
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
}
