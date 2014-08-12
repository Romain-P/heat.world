package org.heat.world.items;

import com.ankamagames.dofus.datacenter.items.Item;
import com.ankamagames.dofus.network.enums.CharacterInventoryPositionEnum;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import lombok.SneakyThrows;
import org.fungsi.concurrent.Future;
import org.fungsi.concurrent.Futures;
import org.fungsi.concurrent.Worker;
import org.heat.data.Datacenter;
import org.heat.shared.database.JdbcRepository;
import org.heat.shared.io.IO;
import org.heat.shared.stream.ImmutableCollectors;
import org.heat.shared.stream.MoreCollectors;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public final class JdbcItemRepository extends JdbcRepository implements WorldItemRepository {
    private final DataSource dataSource;
    private final Worker worker;
    private final Datacenter datacenter;

    private final AtomicInteger idGenerator = new AtomicInteger();

    @Inject
    public JdbcItemRepository(DataSource dataSource, Worker worker, Datacenter datacenter) {
        this.dataSource = dataSource;
        this.worker = worker;
        this.datacenter = datacenter;
    }

    ImmutableList<String> columns = ImmutableList.of(
            "uid",
            "version",
            "template_id",
            "effects",
            "position",
            "quantity"
    );

    ImmutableSet<WorldItemEffect> importEffects(ResultSet rset) {
        return ImmutableSet.of(); // TODO(world/items): import effects from database
    }

    @SneakyThrows
    void exportEffects(PreparedStatement s, int index, ImmutableSet<WorldItemEffect> effects) {
        s.setBlob(index, IO.asBlob(new byte[0])); // TODO(world/items): export effects to database
    }

    @SneakyThrows
    WorldItem importFromDb(ResultSet rset) {
        return WorldItem.create(
                rset.getInt("uint"),
                rset.getLong("version"),
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
        s.setLong(index++, item.getVersion());
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
                execute(simpleInsert("items", columns), newItem, this::exportToDb);
                return newItem;
            });
        } else {
            return worker.submit(() -> {
                WorldItem newItem = item.withNewVersion();
                execute(simpleUpdate("items", "uid", columns), newItem, this::exportToDb);
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
