package org.heat.world;

import com.github.blackrush.acara.CoreEventBus;
import com.github.blackrush.acara.EventBusBuilder;
import com.github.blackrush.acara.SupervisedEventModule;
import com.github.blackrush.acara.supervisor.Supervisor;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import org.fungsi.concurrent.Workers;
import org.heat.StdDataModule;
import org.rocket.network.acara.RocketAcara;
import org.rocket.world.StdBackendControllerModule;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class StdWorldModule extends AbstractModule {
    @Override
    protected void configure() {
        install(new StdDistBackendModule(new StdBackendControllerModule()));
        install(new StdDistFrontendModule(new StdFrontendControllerModule()));
        install(new StdDataModule());
        install(new StdBackendModule());
        install(new StdJdbcModule());
        install(new StdUsersModule());
        install(new StdPlayersModule());
        install(new StdWorldEnvironmentModule());
        install(new StdItemsModule());
    }

    @Provides
    @Singleton
    ExecutorService provideExecutorService() {
        return Executors.newFixedThreadPool(16);
    }

    @Provides
    @Singleton
    ScheduledExecutorService provideScheduler() {
        return Executors.newScheduledThreadPool(2);
    }

    @Provides
    @Named("main")
    Supervisor provideMainSupervisor() {
        return new StdWorldSupervisor();
    }

    @Provides
    EventBusBuilder provideEventBusBuilder(ExecutorService executor, @Named("main") Supervisor supervisor) {
        return CoreEventBus.builder()
                .setWorker(Workers.wrap(executor))
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
}
