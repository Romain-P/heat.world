package org.heat.world.users;

import org.fungsi.Unit;
import org.fungsi.concurrent.Future;
import org.fungsi.concurrent.Futures;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("deprecated")
public final class PermLazyWorldUserRepository implements WorldUserRepository, WorldUserRepository.Unsafe {
    private final WorldUserRepository repository;

    private final Map<Integer, WorldUser> cache = new ConcurrentHashMap<>();

    @Inject
    public PermLazyWorldUserRepository(@Named("base") WorldUserRepository repository) {
        this.repository = repository;
    }

    @Override
    public Future<WorldUser> find(int id) {
        WorldUser user = cache.get(id);
        if (user != null) {
            return Futures.success(user);
        }

        return repository.find(id)
            .onSuccess(x -> cache.put(id, x));
    }

    @Override
    public Future<Unit> save(WorldUser user) {
        if (cache.containsKey(user.getId())) {
            repository.getUnsafe().update(user);
            return Futures.unit();
        }

        return isPresent(user)
            .flatMap(x -> x != null && x
                ? repository.getUnsafe().insert(user)
                : repository.getUnsafe().update(user))
            .toUnit();
    }

    @Override
    public Unsafe getUnsafe() {
        return this;
    }

    @Override
    public Future<Boolean> isPresent(WorldUser user) {
        if (cache.containsKey(user.getId())) {
            return Futures.success(Boolean.TRUE);
        }

        return repository.getUnsafe().isPresent(user).rescue(x -> Boolean.FALSE);
    }

    @Override
    public Future<WorldUser> insert(WorldUser user) {
        cache.put(user.getId(), user);
        return repository.getUnsafe().insert(user);
    }

    @Override
    public Future<WorldUser> update(WorldUser user) {
        return repository.getUnsafe().update(user);
    }
}
