package org.heat.world;

import com.google.inject.PrivateModule;
import com.google.inject.Provides;
import com.google.inject.name.Names;
import org.fungsi.concurrent.Worker;
import org.fungsi.concurrent.Workers;
import org.heat.world.players.JdbcPlayerRepository;
import org.heat.world.players.PermLazyPlayerRepository;
import org.heat.world.players.PlayerRepository;

import java.util.concurrent.ExecutorService;

public class StdJdbcPlayersModule extends PrivateModule {
    @Override
    protected void configure() {
        bind(PlayerRepository.class).annotatedWith(Names.named("base")).to(JdbcPlayerRepository.class).asEagerSingleton();
        bind(PlayerRepository.class).to(PermLazyPlayerRepository.class).asEagerSingleton();

        expose(PlayerRepository.class);
    }

    @Provides
    Worker providePlayerRepositoryWorker(ExecutorService executor) {
        return Workers.wrap(executor);
    }
}
