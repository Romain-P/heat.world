package org.heat.world.users.jdbc;

import com.google.common.collect.ImmutableList;
import org.heat.shared.database.NamedPreparedStatement;
import org.heat.shared.database.Table;
import org.heat.world.users.UserRepository;
import org.heat.world.users.WorldUser;

import javax.inject.Inject;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class WorldUserTable implements Table<WorldUser> {
    @Inject UserRepository userRepository;

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
    public WorldUser importFromDb(ResultSet rset) throws SQLException {
        int id = rset.getInt("id");
        return new WorldUser(userRepository.find(id).get());
    }

    @Override
    public void insertToDb(NamedPreparedStatement s, WorldUser val) throws SQLException {

    }

    @Override
    public void updateToDb(NamedPreparedStatement s, WorldUser val) throws SQLException {

    }
}
