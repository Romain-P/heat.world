package org.heat.world.backend;

import org.fungsi.concurrent.Future;
import org.heat.User;
import org.heat.world.users.UserRepository;

import java.time.Instant;

public interface BackendUserRepository extends UserRepository {
    Future<User> findOrRefresh(int id, Instant updatedAt);
    void push(User user);
}
