package org.heat.world.items;

import lombok.SneakyThrows;
import org.fungsi.Throwables;
import org.fungsi.concurrent.Future;
import org.fungsi.concurrent.Worker;
import org.heat.shared.database.JdbcRepositoryNG;
import org.heat.shared.database.Table;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Singleton
public final class JdbcItemRepository extends JdbcRepositoryNG<WorldItem> implements WorldItemRepository {
    private final DataSource dataSource;

    private final AtomicInteger nextUid;

    @Inject
    public JdbcItemRepository(Table<WorldItem> table, Worker worker, DataSource dataSource) {
        super(table, worker);
        this.dataSource = dataSource;
        this.nextUid = new AtomicInteger(loadNextUid(dataSource));
    }

    String createSelectMultipleQuery(IntStream stream) {
        return stream.mapToObj(uid -> "uid=" + uid)
                .collect(Collectors.joining(" OR ", getSelectQuery() + " ", ""));
    }

    @SneakyThrows
    static int loadNextUid(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("select max(uid) as uid from items"))
        {
            return resultSet.next()
                    ? resultSet.getInt("uid")
                    : 0;
        } catch (SQLException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    protected WorldItem pipelineInsert(WorldItem val) {
        return val.withUid(nextUid.incrementAndGet());
    }

    @Override
    protected WorldItem pipelineUpdate(WorldItem val) {
        return val.withNewVersion();
    }

    @Override
    protected WorldItem pipelineDelete(WorldItem val) {
        return val.withUid(0);
    }

    @SneakyThrows
    @Override
    protected Connection getConnection() {
        return dataSource.getConnection();
    }

    @Override
    public Future<WorldItem> find(int uid) {
        return findFirstByIntColumn("uid", uid);
    }

    @Override
    public Future<List<WorldItem>> find(IntStream stream) {
        // TODO(world/items): split too much uids into sub-queries
        return findList(createSelectMultipleQuery(stream), s -> {});
    }

    @Override
    public Future<WorldItem> save(WorldItem item) {
        return item.getUid() == 0
                ? insert(item)
                : update(item)
                ;
    }

    @Override
    public Future<WorldItem> remove(WorldItem item) {
        return delete(item);
    }
}
