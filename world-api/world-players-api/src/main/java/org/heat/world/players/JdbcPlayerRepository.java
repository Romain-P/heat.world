package org.heat.world.players;

import com.ankamagames.dofus.datacenter.breeds.Breed;
import com.ankamagames.dofus.datacenter.spells.Spell;
import com.ankamagames.dofus.network.enums.DirectionsEnum;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import lombok.SneakyThrows;
import org.fungsi.Throwables;
import org.fungsi.concurrent.Worker;
import org.heat.data.Datacenter;
import org.heat.shared.Collections;
import org.heat.shared.database.JdbcRepository;
import org.heat.shared.stream.MoreCollectors;
import org.heat.world.items.WorldItem;
import org.heat.world.items.WorldItemRepository;
import org.heat.world.metrics.Experience;
import org.heat.world.roleplay.WorldActorLook;
import org.heat.world.roleplay.environment.WorldMapPoint;
import org.heat.world.roleplay.environment.WorldPositioningSystem;

import javax.inject.Inject;
import javax.inject.Named;
import javax.sql.DataSource;
import java.sql.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.heat.world.metrics.GameStats.*;

public final class JdbcPlayerRepository extends JdbcRepository implements PlayerRepository {
    private final DataSource dataSource;
    private final Worker worker;
    private final Datacenter datacenter;
    private final Experience experience;
    private final WorldPositioningSystem wps;
    private final WorldItemRepository items;

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
            WorldPositioningSystem wps,
            WorldItemRepository items
    ) {
        this.dataSource = dataSource;
        this.worker = worker;
        this.datacenter = datacenter;
        this.experience = experience;
        this.wps = wps;
        this.items = items;
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
            "summonableCreatures",
            "spells",
            "kamas"
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
            "summonableCreatures",
            "spells",
            "kamas"
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
    private PlayerStatBook buildPlayerStats(Breed breed, ResultSet rset) {
        PlayerStatBook stats = new DefaultPlayerStatBook(
                breed,
                rset.getInt("statsPoints"),
                rset.getInt("spellsPoints"),
                rset.getShort("strength"),
                rset.getShort("vitality"),
                rset.getShort("wisdom"),
                rset.getShort("chance"),
                rset.getShort("agility"),
                rset.getShort("intelligence")
        );
        stats.get(LIFE).setCurrentAndMax(rset.getShort("life"));
        stats.get(ENERGY).setCurrentAndMax(rset.getShort("energy"), rset.getShort("maxEnergy"));
        stats.get(ACTIONS).setBase(rset.getShort("actions"));
        stats.get(MOVEMENTS).setBase(rset.getShort("movements"));
        stats.get(PROSPECTING).setBase(rset.getShort("prospecting"));
        stats.get(SUMMONABLE_CREATURES).setBase(rset.getShort("summonableCreatures"));
        return stats;
    }

    @SneakyThrows
    private PlayerSpellBook buildPlayerSpells(ResultSet rset) {
        List<PlayerSpell> spells = new LinkedList<>();

        Struct[] array = (Struct[]) rset.getArray("spells").getArray();
        for (int i = 0; i < array.length; i++) {
            // extract
            Struct struct = array[i];
            Object[] attr = struct.getAttributes();
            int spellId = (Integer) attr[0];
            int spellLevelInt = (Integer) attr[1];
            int spellPositionInt = (Integer) attr[2];

            // refine
            Spell spellData = datacenter.find(Spell.class, spellId).get();
            byte spellLevel = (byte) spellLevelInt;
            OptionalInt spellPosition = spellPositionInt != -1
                    ? OptionalInt.of(spellPositionInt)
                    : OptionalInt.empty();
            int spellMinPlayerLevel = Players.getSpellMinPlayerLevel(i);

            // use
            spells.add(PlayerSpell.create(spellData, spellMinPlayerLevel, spellLevel, spellPosition));
        }

        return DefaultPlayerSpellBook.create(spells);
    }

    @SneakyThrows
    private Object exportPlayerSpell(Connection co, PlayerSpell spell) {
        return co.createStruct("player_spell", new Object[] {
                spell.getId(),
                (int) spell.getLevel(),
                spell.getPosition().orElse(-1)
        });
    }

    @SneakyThrows
    private Array exportPlayerSpells(Connection co, PlayerSpellBook book) {
        return co.createArrayOf("player_spell", book.getSpellStream()
            .map(spell -> exportPlayerSpell(co, spell))
            .toArray());
    }

    @SneakyThrows
    private List<WorldItem> importPlayerItems(Connection connection, int id) {
        String sql =
            "select item_uid " +
            "from player_items " +
            "where player_id=?";


        IntStream.Builder uids = IntStream.builder();

        PreparedStatement s = connection.prepareStatement(sql);
        s.setInt(1, id);

        try (ResultSet subrset = s.executeQuery()) {
            while (subrset.next()) {
                uids.add(subrset.getInt(1));
            }
        } catch (SQLException e) {
            throw Throwables.propagate(e);
        }

        return items.find(uids.build()).get(Duration.ofMillis(1000));
    }

    @SneakyThrows
    private PlayerItemWallet buildWallet(ResultSet rset, int id) {
        PlayerItemWallet wallet = new HashPlayerItemWallet();
        wallet.setKamas(rset.getInt("kamas"));
        wallet.addAll(importPlayerItems(rset.getStatement().getConnection(), id));
        return wallet;
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
        player.setStats(buildPlayerStats(player.getBreed(), rset));
        player.setSpells(buildPlayerSpells(rset));
        player.setWallet(buildWallet(rset, player.getId()));
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
        s.setArray(index++, exportPlayerSpells(s.getConnection(), player.getSpells()));
        s.setInt(index++, player.getWallet().getKamas());
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
        s.setArray(index++, exportPlayerSpells(s.getConnection(), player.getSpells()));
        s.setInt(index++, player.getWallet().getKamas());
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
