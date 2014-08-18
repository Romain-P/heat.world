package org.heat.world.players;

import org.fungsi.Unit;
import org.fungsi.concurrent.Future;
import org.fungsi.concurrent.Futures;
import org.heat.world.items.WorldItem;
import org.heat.world.items.WorldItemRepository;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.IntStream;

public final class JdbcPlayerItemRepository implements PlayerItemRepository {
    private final DataSource dataSource;
    private final WorldItemRepository items;

    @Inject
    public JdbcPlayerItemRepository(DataSource dataSource, WorldItemRepository items) {
        this.dataSource = dataSource;
        this.items = items;
    }

    @Override
    public Future<List<WorldItem>> findItemsByPlayer(int playerId) {
        try (Connection co = dataSource.getConnection()) {
            try (PreparedStatement s = co.prepareStatement("select item_id from player_items where player_id=?")) {
                s.setInt(1, playerId);

                IntStream.Builder uids = IntStream.builder();
                try (ResultSet rset = s.executeQuery()) {
                    while (rset.next()) {
                        int uid = rset.getInt(1);
                        uids.add(uid);
                    }
                }

                return items.find(uids.build());
            }
        } catch (SQLException e) {
            throw new Error(e);
        }
    }

    @Override
    public Future<Unit> persist(int playerId, int itemId) {
        try (Connection co = dataSource.getConnection()) {
            try (PreparedStatement s = co.prepareStatement("insert into player_items(player_id, item_id) values(?, ?)")) {
                s.setInt(1, playerId);
                s.setInt(2, itemId);

                s.executeUpdate();
                return Futures.unit();
            }
        } catch (SQLException e) {
            throw new Error(e);
        }
    }

    @Override
    public Future<Unit> remove(int playerId, int itemId) {
        try (Connection co = dataSource.getConnection()) {
            try (PreparedStatement s = co.prepareStatement("delete from player_items where player_id=? and item_id=?")) {
                s.setInt(1, playerId);
                s.setInt(2, itemId);

                s.executeUpdate();
                return Futures.unit();
            }
        } catch (SQLException e) {
            throw new Error(e);
        }
    }
}
