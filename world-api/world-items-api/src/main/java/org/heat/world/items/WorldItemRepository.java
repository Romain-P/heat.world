package org.heat.world.items;

import org.fungsi.concurrent.Future;

import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public interface WorldItemRepository {
    Optional<WorldItem> find(int uid);
    Stream<WorldItem> find(IntStream uids);

    Future<WorldItem> save(WorldItem item);
    Future<WorldItem> remove(WorldItem item);
}
