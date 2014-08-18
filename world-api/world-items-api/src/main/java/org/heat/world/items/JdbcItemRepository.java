package org.heat.world.items;

import com.ankamagames.dofus.datacenter.items.Item;
import com.ankamagames.dofus.network.enums.CharacterInventoryPositionEnum;
import com.ankamagames.dofus.network.types.game.data.items.effects.ObjectEffect;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import lombok.SneakyThrows;
import org.fungsi.Throwables;
import org.fungsi.concurrent.Future;
import org.fungsi.concurrent.Futures;
import org.fungsi.concurrent.Worker;
import org.heat.data.Datacenter;
import org.heat.dofus.network.NetworkComponentFactory;
import org.heat.shared.database.JdbcRepository;
import org.heat.shared.io.AutoGrowingWriter;
import org.heat.shared.io.DataReader;
import org.heat.shared.io.HeapDataReader;
import org.heat.shared.io.IO;
import org.heat.shared.stream.ImmutableCollectors;
import org.heat.shared.stream.MoreCollectors;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.sql.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public final class JdbcItemRepository extends JdbcRepository implements WorldItemRepository {
    private final DataSource dataSource;
    private final Worker worker;
    private final Datacenter datacenter;
    private final NetworkComponentFactory<ObjectEffect> effectFactory;

    private final AtomicInteger idGenerator = new AtomicInteger();

    @Inject
    public JdbcItemRepository(DataSource dataSource, Worker worker, Datacenter datacenter, NetworkComponentFactory<ObjectEffect> effectFactory) {
        this.dataSource = dataSource;
        this.worker = worker;
        this.datacenter = datacenter;
        this.effectFactory = effectFactory;
    }

    ImmutableList<String> columns = ImmutableList.of(
            "uid",
            "gid",
            "effects",
            "position",
            "quantity"
    );

    @SneakyThrows
    ImmutableSet<WorldItemEffect> importEffects(ResultSet rset) {
        Blob blob = rset.getBlob("effects");
        try {
            ImmutableSet.Builder<WorldItemEffect> effects = ImmutableSet.builder();

            byte[] bytes = IO.readAll(blob.getBinaryStream()::read);
            DataReader reader = new HeapDataReader(bytes, 0, bytes.length);
            while (reader.canRead(2)) {
                int typeId = reader.read_ui16();
                ObjectEffect effect = effectFactory.create(typeId).get();
                effect.deserialize(reader);

                effects.add(Effects.fromObjectEffect(effect));
            }

            return effects.build();
        } finally {
            blob.free();
        }
    }

    @SneakyThrows
    void exportEffects(PreparedStatement s, int index, ImmutableSet<WorldItemEffect> effects) {
        AutoGrowingWriter writer = new AutoGrowingWriter();
        for (WorldItemEffect effect : effects) {
            ObjectEffect e = effect.toObjectEffect();
            writer.write_ui16(e.getProtocolId());
            e.serialize(writer);
        }
        s.setBytes(index, writer.toByteArray()); // TODO(world/items): convert AutoGrowingWriter to InputStream
    }

    @SneakyThrows
    WorldItem importFromDb(ResultSet rset) {
        return WorldItem.create(
                rset.getInt("uint"),
                0,
                datacenter.find(Item.class, rset.getInt("template_id")).get(),
                importEffects(rset),
                CharacterInventoryPositionEnum.valueOf(rset.getInt("position")).get(),
                rset.getInt("quantity")
        );
    }

    @SuppressWarnings("UnusedAssignment")
    @SneakyThrows
    void exportToDb(WorldItem item, PreparedStatement s) {
        int index = 1;
        s.setInt(index++, item.getUid());
        s.setInt(index++, item.getTemplate().getId());
        exportEffects(s, index++, item.getEffects());
        s.setInt(index++, item.getPosition().value);
        s.setInt(index++, item.getQuantity());
    }

    @SneakyThrows
    @Override
    protected Connection getConnection() {
        return dataSource.getConnection();
    }

    Stream<WorldItem> query(String rest) {
        return query(simpleSelect("items", columns) + " " + rest, s -> {
        }, this::importFromDb);
    }

    String buildBatchQuery(int[] uids) {
        StringBuilder sb = new StringBuilder();
        sb.append("WHERE uid=");
        sb.append(uids[0]);
        for (int i = 1; i < uids.length; i++) {
            sb.append(" OR uid=").append(uids[i]);
        }
        return sb.toString();
    }

    void doExecute(WorldItem item, String query) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement s = connection.prepareStatement(query)) {
                exportToDb(item, s);
                s.execute();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.commit();
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw Throwables.propagate(e);
        }
    }

    void doInsert(WorldItem item) {
        doExecute(item, simpleInsert("items", columns));
    }

    void doUpdate(WorldItem item) {
        doExecute(item, simpleUpdate("items", "uid", columns));
    }

    @Override
    public Future<WorldItem> find(int uid) {
        return worker.submit(() -> {
            try (Stream<WorldItem> stream = query("WHERE uid=" + uid)) {
                return stream.collect(MoreCollectors.unique());
            }
        });
    }

    @Override
    public Future<List<WorldItem>> find(IntStream uids) {
        int[] arr = uids.toArray();
        if (arr.length <= 0) {
            return Futures.success(ImmutableList.of());
        } else if (arr.length == 1) {
            return find(arr[0]).map(ImmutableList::of);
        }

        String query = buildBatchQuery(arr);

        return worker.submit(() -> {
            try (Stream<WorldItem> stream = query(query)) {
                return stream.collect(ImmutableCollectors.toList());
            }
        });
    }

    @Override
    public Future<WorldItem> save(WorldItem item) {
        if (item.getUid() == 0) {
            return worker.submit(() -> {
                WorldItem newItem = item.withUid(idGenerator.incrementAndGet());
                doInsert(newItem);
                return newItem;
            });
        } else {
            return worker.submit(() -> {
                WorldItem newItem = item.withNewVersion();
                doUpdate(newItem);
                return newItem;
            });
        }
    }

    @Override
    public Future<WorldItem> remove(WorldItem item) {
        return worker.submit(() -> {
            WorldItem newItem = item.withUid(0);
            execute(simpleDelete("items", "uid"), s -> s.setInt(1, item.getUid()));
            return newItem;
        });
    }
}
