package org.heat.world;

import com.github.blackrush.acara.EventBusBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.typesafe.config.Config;
import org.heat.data.MapDatacenter;
import org.heat.shared.database.Repository;
import org.heat.world.roleplay.environment.WorldMap;
import org.heat.world.roleplay.environment.WorldMapRepositoryImpl;
import org.heat.world.roleplay.environment.WorldPositioningSystem;
import org.heat.world.roleplay.environment.WorldPositioningSystemImpl;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class StdWorldEnvironmentModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(WorldPositioningSystem.class).to(WorldPositioningSystemImpl.class).asEagerSingleton();
    }

    @Provides
    @Singleton
    Repository<WorldMap> provideWorldMapRepository(
            MapDatacenter datacenter,
            EventBusBuilder eventBusBuilder,
            Config config
    ) {
        Duration loadTimeout = Duration.ofNanos(config.getDuration("heat.world.maps.load-timeout", TimeUnit.NANOSECONDS));

        return new WorldMapRepositoryImpl(datacenter, loadTimeout, eventBusBuilder::build);
    }
}
