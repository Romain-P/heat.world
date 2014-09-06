package org.heat.world.users;

import org.fungsi.concurrent.Future;
import org.heat.User;

public interface UserRepository {
    Future<User> find(int id);
}
