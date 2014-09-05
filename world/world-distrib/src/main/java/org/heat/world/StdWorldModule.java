package org.heat.world;

import com.github.blackrush.acara.CoreEventBus;
import com.github.blackrush.acara.EventBusBuilder;
import com.github.blackrush.acara.SupervisedEventModule;
import com.github.blackrush.acara.supervisor.Supervisor;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.typesafe.config.Config;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import org.rocket.network.acara.RocketAcara;
import org.rocket.world.StdBackendControllerModule;

import java.time.Clock;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class StdWorldModule extends AbstractModule {
    @Override
    protected void configure() {
        install(new StdDistBackendModule(new StdBackendControllerModule()));
        install(new StdDistFrontendModule(new StdFrontendControllerModule()));
        install(new StdDatacenterModule());
        install(new StdBackendModule());
        install(new StdJdbcModule());
        install(new StdUsersModule());
        install(new StdPlayersModule());
        install(new StdWorldEnvironmentModule());
        install(new StdItemsModule());
        install(new StdPlayerTradeModule());
        install(new StdClassicalGroupModule());
    }

    @Provides
    @Singleton
    ExecutorService provideExecutorService(Config config) {
        int parallelism = config.getInt("heat.world.workers-parallelism");
        if (parallelism <= 0) {
            parallelism = Runtime.getRuntime().availableProcessors();
        }
        return Executors.newWorkStealingPool(parallelism);
    }

    @Provides
    @Singleton
    ScheduledExecutorService provideScheduler(Config config) {
        int coreSize = config.getInt("heat.world.scheduler-core-size");
        return Executors.newScheduledThreadPool(coreSize);
    }

    @Provides
    @Named("main")
    Supervisor provideMainSupervisor() {
        return new StdWorldSupervisor();
    }

    @Provides
    EventBusBuilder provideEventBusBuilder(ExecutorService executor, @Named("main") Supervisor supervisor) {
        return CoreEventBus.builder()
                .setWorker(new WorldWorker(executor))
                .setSupervisor(supervisor)
                .install(new SupervisedEventModule())
                .install(RocketAcara.newContextfulModule())
                ;
    }

    @Provides
    @Singleton
    ByteBufAllocator provideByteBufAllocator() {
        return new PooledByteBufAllocator(/*preferDirect*/true);
    }

    @Provides
    Clock provideClock() {
        return Clock.systemUTC();
    }
}
