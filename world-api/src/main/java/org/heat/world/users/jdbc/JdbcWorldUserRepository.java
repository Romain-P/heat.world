package org.heat.world.users.jdbc;

import lombok.SneakyThrows;
import org.fungsi.Unit;
import org.fungsi.concurrent.Future;
import org.fungsi.concurrent.Futures;
import org.fungsi.concurrent.Worker;
import org.heat.User;
import org.heat.shared.database.JdbcRepositoryNG;
import org.heat.shared.database.Table;
import org.heat.world.users.UserRepository;
import org.heat.world.users.WorldUser;
import org.heat.world.users.WorldUserRepository;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;

@SuppressWarnings("deprecated")
public final class JdbcWorldUserRepository extends JdbcRepositoryNG<WorldUser>
    implements WorldUserRepository,
           WorldUserRepository.Unsafe
{
    private final DataSource dataSource;
    private final UserRepository userRepository;

    @Inject
    public JdbcWorldUserRepository(Table<WorldUser> table, Worker worker, DataSource dataSource, UserRepository userRepository) {
        super(table, worker);
        this.dataSource = dataSource;
        this.userRepository = userRepository;
    }

    @SneakyThrows
    @Override
    protected Connection getConnection() {
        return dataSource.getConnection();
    }

    @Override
    public Unsafe getUnsafe() {
        return this;
    }

    private Future<WorldUser> create(User user) {
        WorldUser wuser = new WorldUser();
        wuser.setId(user.getId());
        wuser.setUser(user);

        return insert(wuser);
    }

    private Future<WorldUser> findOrCreate(User user) {
        return findFirstByIntColumn("id", user.getId())
            .map(worldUser -> {
                worldUser.setUser(user);
                return worldUser;
            })
            .mayRescue(err ->
                create(user)
            );
    }

    @Override
    public Future<WorldUser> find(int id) {
        return userRepository.find(id)
            .flatMap(this::findOrCreate);
    }

    @Override
    public Future<WorldUser> findOrRefresh(int id, Instant updatedAt) {
        return userRepository.findOrRefresh(id, updatedAt)
            .<WorldUser>flatMap(this::findOrCreate);
    }

    @Override
    public Future<Unit> save(WorldUser user) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Future<Boolean> isPresent(WorldUser user) {
        return getWorker().submit(() -> {
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement("select count(id) from world_users where id=?")) {
                statement.setInt(1, user.getId());

                try (ResultSet resultSet = statement.executeQuery()) {
                    resultSet.next();
                    return resultSet.getInt(1) > 0 ? Boolean.TRUE : Boolean.FALSE;
                }
            }
        });
    }

    @Override
    public Future<WorldUser> update(WorldUser val) {
//        return super.update(val);
        return Futures.success(val); // there is nothing to update for now
    }

    @Override
    public Future<WorldUser> insert(WorldUser val) {
        return super.insert(val);
    }
}
