package org.heat.world.roleplay.environment;

import com.github.blackrush.acara.EventBus;
import com.google.common.collect.Maps;
import org.heat.datacenter.MapDatacenter;
import org.heat.dofus.d2p.data.MapData;
import org.heat.shared.database.Repository;

import javax.inject.Provider;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

public final class WorldMapRepositoryImpl implements Repository<WorldMap> {
    private final MapDatacenter datacenter;
    private final Duration loadTimeout;
    private final Provider<EventBus> eventBusProvider;
    private final Map<Long, WorldMap> maps = Maps.newConcurrentMap();

    public WorldMapRepositoryImpl(MapDatacenter datacenter, Duration loadTimeout, Provider<EventBus> eventBusProvider) {
        this.datacenter = datacenter;
        this.loadTimeout = loadTimeout;
        this.eventBusProvider = eventBusProvider;
    }

    @Override
    public Optional<WorldMap> find(long id) {
        WorldMap map = maps.get(id);

        if (map == null) {
            MapData data = datacenter.fetch(id).get(loadTimeout);
            EventBus eventBus = eventBusProvider.get();
            map = new WorldMap(eventBus, data);
            maps.put(id, map);
        }

        return Optional.of(map);
    }
}
