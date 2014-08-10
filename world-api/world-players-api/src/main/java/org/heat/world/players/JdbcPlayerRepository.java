package org.heat.world.players;

import com.ankamagames.dofus.datacenter.breeds.Breed;
import com.ankamagames.dofus.network.enums.DirectionsEnum;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import lombok.SneakyThrows;
import org.fungsi.concurrent.Worker;
import org.heat.data.Datacenter;
import org.heat.shared.Collections;
import org.heat.shared.database.JdbcRepository;
import org.heat.shared.stream.MoreCollectors;
import org.heat.world.metrics.Experience;
import org.heat.world.roleplay.WorldActorLook;
import org.heat.world.roleplay.environment.WorldMapPoint;
import org.heat.world.roleplay.environment.WorldPositioningSystem;

import javax.inject.Inject;
import javax.inject.Named;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.heat.world.metrics.GameStats.*;

public final class JdbcPlayerRepository extends JdbcRepository implements PlayerRepository {
    private final DataSource dataSource;
    private final Worker worker;
    private final Datacenter datacenter;
    private final Experience experience;
    private final WorldPositioningSystem wps;

    private final Map<Integer, Player> cache = Maps.newConcurrentMap();
    private final Map<Integer, LinkedList<Player>> cacheByUserId = Maps.newConcurrentMap();
    private final Map<String, Player> cacheByName = Maps.newConcurrentMap();
    private final AtomicInteger idGenerator = new AtomicInteger();

    @Inject
    public JdbcPlayerRepository(
            DataSource dataSource,
            @Named("player-repository") Worker worker,
            Datacenter datacenter,
            @Named("player") Experience experience,
            WorldPositioningSystem wps
    ) {
        this.dataSource = dataSource;
        this.worker = worker;
        this.datacenter = datacenter;
        this.experience = experience;
        this.wps = wps;
        initIdGenerator();
    }

    ImmutableList<String> fields = ImmutableList.of(
            "id",
            "userId",
            "name",
            "breedId",
            "sex",
            "lookId",
            "headId",
            "scale",
            "colors",
            "mapId",
            "cellId",
            "directionId",
            "experience",
            "statsPoints",
            "spellsPoints",
            "strength",
            "vitality",
            "wisdom",
            "chance",
            "agility",
            "intelligence",
            "life",
            "energy",
            "maxEnergy",
            "actions",
            "movements",
            "prospecting",
            "summonableCreatures"
    );
    ImmutableList<String> modifiableFields = ImmutableList.of(
            "sex",
            "lookId",
            "headId",
            "scale",
            "colors",
            "mapId",
            "cellId",
            "directionId",
            "experience",
            "statsPoints",
            "spellsPoints",
            "strength",
            "vitality",
            "wisdom",
            "chance",
            "agility",
            "intelligence",
            "life",
            "energy",
            "maxEnergy",
            "actions",
            "movements",
            "prospecting",
            "summonableCreatures"
    );

    private void initIdGenerator() {
        try (Stream<Integer> stream = query("select max(id) as lastId from players", rset -> rset.getInt("lastId"))) {
            stream.collect(MoreCollectors.uniqueOption())
                    .ifPresent(idGenerator::set);
        }
    }

    private PlayerExperience buildPlayerExperience(double experience) {
        Experience step = this.experience.getNextUntilEnoughExperience(experience);
        return new PlayerExperience(experience, step);
    }

    @SneakyThrows
    private Player importFromDb(ResultSet rset) {
        Player player = new Player();
        player.setId(rset.getInt("id"));
        player.setUserId(rset.getInt("userId"));
        player.setName(rset.getString("name"));
        player.setBreed(datacenter.find(Breed.class, rset.getInt("breedId")).get());
        player.setSex(rset.getBoolean("sex"));
        player.setLook(new WorldActorLook(
                WorldActorLook.Bones.STANDING,
                rset.getShort("lookId"),
                rset.getShort("headId"),
                rset.getShort("scale"),
                asIntArray(rset.getArray("colors"))
        ));
        player.setPosition(wps.locate(
                rset.getInt("mapId"),
                WorldMapPoint.of(rset.getInt("cellId")).get(),
                DirectionsEnum.valueOf(rset.getInt("directionId")).get()
        ));
        player.setExperience(buildPlayerExperience(rset.getDouble("experience")));
        player.setStats(new DefaultPlayerStatBook(
                player.getBreed(),
                rset.getInt("statsPoints"),
                rset.getInt("spellsPoints"),
                rset.getShort("strength"),
                rset.getShort("vitality"),
                rset.getShort("wisdom"),
                rset.getShort("chance"),
                rset.getShort("agility"),
                rset.getShort("intelligence")
        ));
        player.getStats().get(LIFE).setCurrentAndMax(rset.getShort("life"));
        player.getStats().get(ENERGY).setCurrentAndMax(rset.getShort("energy"), rset.getShort("maxEnergy"));
        player.getStats().get(ACTIONS).setBase(rset.getShort("actions"));
        player.getStats().get(MOVEMENTS).setBase(rset.getShort("movements"));
        player.getStats().get(PROSPECTING).setBase(rset.getShort("prospecting"));
        player.getStats().get(SUMMONABLE_CREATURES).setBase(rset.getShort("summonableCreatures"));
        return player;
    }

    @SuppressWarnings("UnusedAssignment")
    @SneakyThrows
    private void insertToDb(Player player, PreparedStatement s) {
        int index = 1;
        
        s.setInt(index++, player.getId());
        s.setInt(index++, player.getUserId());
        s.setString(index++, player.getName());
        s.setInt(index++, player.getBreed().getId());
        s.setBoolean(index++, player.getSex());
        s.setInt(index++, player.getLook().getLookId());
        s.setInt(index++, player.getLook().getHeadId());
        s.setInt(index++, player.getLook().getScale());
        s.setObject(index++, player.getLook().getColors());
        s.setInt(index++, player.getPosition().getMapId());
        s.setInt(index++, player.getPosition().getCellId());
        s.setInt(index++, player.getPosition().getDirection().value);
        s.setDouble(index++, player.getExperience().getCurrent());
        s.setInt(index++, player.getStats().getStatsPoints());
        s.setInt(index++, player.getStats().getSpellsPoints());
        s.setShort(index++, player.getStats().get(STRENGTH).getBase());
        s.setShort(index++, player.getStats().get(VITALITY).getBase());
        s.setShort(index++, player.getStats().get(WISDOM).getBase());
        s.setShort(index++, player.getStats().get(CHANCE).getBase());
        s.setShort(index++, player.getStats().get(AGILITY).getBase());
        s.setShort(index++, player.getStats().get(INTELLIGENCE).getBase());
        s.setShort(index++, player.getStats().get(LIFE).getCurrent());
        s.setShort(index++, player.getStats().get(ENERGY).getCurrent());
        s.setShort(index++, player.getStats().get(ENERGY).getMax());
        s.setShort(index++, player.getStats().get(ACTIONS).getBase());
        s.setShort(index++, player.getStats().get(MOVEMENTS).getBase());
        s.setShort(index++, player.getStats().get(PROSPECTING).getBase());
        s.setShort(index++, player.getStats().get(SUMMONABLE_CREATURES).getBase());
    }

    @SuppressWarnings("UnusedAssignment")
    @SneakyThrows
    private void updateToDb(Player player, PreparedStatement s) {
        int index = 1;
        
        s.setBoolean(index++, player.getSex());
        s.setInt(index++, player.getLook().getLookId());
        s.setInt(index++, player.getLook().getHeadId());
        s.setInt(index++, player.getLook().getScale());
        s.setObject(index++, player.getLook().getColors());
        s.setInt(index++, player.getPosition().getMapId());
        s.setInt(index++, player.getPosition().getCellId());
        s.setInt(index++, player.getPosition().getDirection().value);
        s.setDouble(index++, player.getExperience().getCurrent());
        s.setInt(index++, player.getStats().getStatsPoints());
        s.setInt(index++, player.getStats().getSpellsPoints());
        s.setShort(index++, player.getStats().get(STRENGTH).getBase());
        s.setShort(index++, player.getStats().get(VITALITY).getBase());
        s.setShort(index++, player.getStats().get(WISDOM).getBase());
        s.setShort(index++, player.getStats().get(CHANCE).getBase());
        s.setShort(index++, player.getStats().get(AGILITY).getBase());
        s.setShort(index++, player.getStats().get(INTELLIGENCE).getBase());
        s.setShort(index++, player.getStats().get(LIFE).getCurrent());
        s.setShort(index++, player.getStats().get(ENERGY).getCurrent());
        s.setShort(index++, player.getStats().get(ENERGY).getMax());
        s.setShort(index++, player.getStats().get(ACTIONS).getBase());
        s.setShort(index++, player.getStats().get(MOVEMENTS).getBase());
        s.setShort(index++, player.getStats().get(PROSPECTING).getBase());
        s.setShort(index++, player.getStats().get(SUMMONABLE_CREATURES).getBase());
        s.setInt(index++, player.getId());
    }

    private void addToCache(Player player) {
        cache.put(player.getId(), player);
        cacheByUserId.computeIfAbsent(player.getUserId(), x -> new LinkedList<>()).add(player);
        cacheByName.put(player.getName(), player);
    }

    private void addToCacheWithCommonOwner(int userId, LinkedList<Player> players) {
        LinkedList<Player> cached = cacheByUserId.get(userId);
        if (cached == null) {
            cacheByUserId.put(userId, Collections.cloneLinkedList(players));
        } else {
            Collections.addAllLinkedList(cached, players);
        }
        for (Player player : players) {
            cache.put(player.getId(), player);
            cacheByName.put(player.getName(), player);
        }
    }

    private void removeFromCache(Player player) {
        cache.remove(player.getId());
        List<Player> byUserId = cacheByUserId.get(player.getUserId());
        if (byUserId != null) {
            byUserId.remove(player);
        }
        cacheByName.remove(player.getName());
    }

    private void doInsert(Player player) {
        addToCache(player);
        execute(simpleInsert("players", fields), player, this::insertToDb);
    }

    private void doUpdate(Player player) {
        execute(simpleUpdate("players", "id", modifiableFields), player, this::updateToDb);
    }

    private void doDelete(Player player) {
        removeFromCache(player);
        execute(simpleDelete("players", "id"), s -> s.setInt(1, player.getId()));
    }

    @SneakyThrows
    @Override
    protected Connection getConnection() {
        return dataSource.getConnection();
    }

    @Override
    public void save(Player o) {
        if (o.getId() == 0) {
            o.setId(idGenerator.incrementAndGet());
//            worker.cast(() -> doInsert(o));
            doInsert(o); // TODO(world/players): for debug purposes only
        } else {
//            worker.cast(() -> doUpdate(o));
            doUpdate(o);
        }
    }

    @Override
    public void remove(Player o) {
        if (cache.containsKey(o.getId())) {
            worker.cast(() -> doDelete(o));
        }
    }

    @Override
    public Optional<Player> find(int id) {
        Player player = cache.get(id);
        if (player != null) {
            return Optional.of(player);
        }

        Optional<Player> opt;
        try (Stream<Player> stream = query(simpleSelect("players", "id", fields), s -> s.setInt(1, id), this::importFromDb)) {
            opt = stream.collect(MoreCollectors.uniqueOption());
        }
        opt.ifPresent(this::addToCache);
        return opt;
    }

    @Override
    public List<Player> findByUserId(int userId) {
        LinkedList<Player> players = cacheByUserId.get(userId);
        if (players != null) {
            return org.heat.shared.Collections.cloneLinkedList(players);
        }

        try (Stream<Player> stream = query(simpleSelect("players", "userId", fields), s -> s.setInt(1, userId), this::importFromDb)) {
            players = stream.collect(Collectors.toCollection(LinkedList::new));
        }
        addToCacheWithCommonOwner(userId, players);
        return players;
    }

    @Override
    public Optional<Player> findByName(String name) {
        Player player = cacheByName.get(name);
        if (player != null) {
            return Optional.of(player);
        }

        Optional<Player> opt;
        try (Stream<Player> stream = query(simpleSelect("players", "name", fields), s -> s.setString(1, name), this::importFromDb)) {
            opt = stream.collect(MoreCollectors.uniqueOption());
        }
        opt.ifPresent(this::addToCache);
        return opt;
    }
}
