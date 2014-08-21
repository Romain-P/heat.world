package org.heat.world.players;

import lombok.SneakyThrows;
import org.fungsi.Unit;
import org.fungsi.concurrent.Future;
import org.fungsi.concurrent.Worker;
import org.heat.shared.database.JdbcRepositoryNG;
import org.heat.shared.database.Table;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public final class JdbcPlayerRepository extends JdbcRepositoryNG<Player> implements PlayerRepository {
    private final DataSource dataSource;

    public JdbcPlayerRepository(Table<Player> table, DataSource dataSource, Worker worker) {
        super(table, worker);
        this.dataSource = dataSource;
    }

    @SneakyThrows
    @Override
    protected Connection getConnection() {
        return dataSource.getConnection();
    }

    @Override
    public Future<AtomicInteger> createIdGenerator() {
        return getWorker().submit(() -> {
            try (Connection connection = getConnection();
                 Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery("select max(id) as id from players"))
            {
                if (!resultSet.next()) {
                    return new AtomicInteger(0);
                }
                return new AtomicInteger(resultSet.getInt("id"));
            }
        });
    }

    @Override
    public Future<Player> find(int id) {
        return findFirstByIntColumn("id", id);
    }

    @Override
    public Future<List<Player>> findByUserId(int userId) {
        return findListByIntColumn("userId", userId);
    }

    @Override
    public Future<Player> findByName(String name) {
        return findFirstByColumn("name", name);
    }

    @Override
    public Future<Unit> create(Player player) {
        return insert(player).toUnit();
    }

    @Override
    public Future<Unit> save(Player player) {
        return update(player).toUnit();
    }

    @Override
    public Future<Unit> remove(Player player) {
        return delete(player).toUnit();
    }
}
