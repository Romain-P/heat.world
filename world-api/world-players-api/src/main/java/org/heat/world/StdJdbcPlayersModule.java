package org.heat.world;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.name.Named;
import org.fungsi.concurrent.Worker;
import org.fungsi.concurrent.Workers;
import org.heat.world.players.JdbcPlayerItemRepository;
import org.heat.world.players.JdbcPlayerRepository;
import org.heat.world.players.PlayerItemRepository;
import org.heat.world.players.PlayerRepository;

import java.util.concurrent.ExecutorService;

public class StdJdbcPlayersModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(PlayerItemRepository.class).to(JdbcPlayerItemRepository.class).asEagerSingleton();
        bind(PlayerRepository.class).to(JdbcPlayerRepository.class).asEagerSingleton();
    }

    @Provides
    @Named("player-repository")
    Worker providePlayerRepositoryWorker(ExecutorService executor) {
        return Workers.wrap(executor);
    }
}
