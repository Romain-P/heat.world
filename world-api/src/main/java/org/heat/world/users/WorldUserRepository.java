package org.heat.world.users;

import org.fungsi.Unit;
import org.fungsi.concurrent.Future;

public interface WorldUserRepository {
    Future<WorldUser> find(int id);
    Future<Unit> save(WorldUser user);

    @Deprecated
    Unsafe getUnsafe();

    interface Unsafe {
        Future<Boolean> isPresent(WorldUser user);
        Future<WorldUser> insert(WorldUser user);
        Future<WorldUser> update(WorldUser user);
    }
}
