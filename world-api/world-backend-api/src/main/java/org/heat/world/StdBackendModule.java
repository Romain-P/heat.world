package org.heat.world;

import com.google.inject.PrivateModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import org.fungsi.concurrent.Timer;
import org.fungsi.concurrent.Timers;
import org.heat.world.backend.Backend;
import org.heat.world.backend.DefaultBackend;

import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;

public class StdBackendModule extends PrivateModule {
    @Override
    protected void configure() {
        bind(Random.class).annotatedWith(Names.named("ticket")).toInstance(new Random(System.nanoTime()));
        bind(Backend.class).to(DefaultBackend.class).asEagerSingleton();
        expose(Backend.class);
    }

    @Provides
    @Singleton
    @Named("user-auth-ttl")
    Timer provideUserAuthTtl(ScheduledExecutorService executor) {
        return Timers.wrap(executor);
    }
}
