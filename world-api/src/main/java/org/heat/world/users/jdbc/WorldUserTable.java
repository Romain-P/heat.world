package org.heat.world.users.jdbc;

import com.google.common.collect.ImmutableList;
import org.fungsi.concurrent.Future;
import org.fungsi.concurrent.Futures;
import org.heat.shared.database.NamedPreparedStatement;
import org.heat.shared.database.Table;
import org.heat.world.users.WorldUser;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class WorldUserTable implements Table<WorldUser> {
    @Override
    public String getTableName() {
        return "users";
    }

    @Override
    public List<String> getPrimaryKeys() {
        return ImmutableList.of("id");
    }

    @Override
    public List<String> getSelectableColumns() {
        return ImmutableList.of();
    }

    @Override
    public List<String> getInsertableColumns() {
        return ImmutableList.of();
    }

    @Override
    public List<String> getUpdatableColumns() {
        return ImmutableList.of();
    }

    @Override
    public void setPrimaryKeys(NamedPreparedStatement s, WorldUser val) throws SQLException {
        s.setInt("id", val.getId());
    }

    @Override
    public Future<WorldUser> importFromDb(ResultSet rset) throws SQLException {
        WorldUser user = new WorldUser();
        user.setId(rset.getInt("id"));

        return Futures.success(user);
    }

    @Override
    public void insertToDb(NamedPreparedStatement s, WorldUser val) throws SQLException {

    }

    @Override
    public void updateToDb(NamedPreparedStatement s, WorldUser val) throws SQLException {

    }
}
