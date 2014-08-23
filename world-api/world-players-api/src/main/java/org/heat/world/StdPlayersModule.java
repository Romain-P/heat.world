package org.heat.world;

import com.github.blackrush.acara.EventBusBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.typesafe.config.Config;
import org.heat.world.metrics.Experience;
import org.heat.world.metrics.Experiences;
import org.heat.world.players.DefaultPlayerFactory;
import org.heat.world.players.DefaultPlayerRegistry;
import org.heat.world.players.PlayerFactory;
import org.heat.world.players.PlayerRegistry;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

public class StdPlayersModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(PlayerFactory.class).to(DefaultPlayerFactory.class).asEagerSingleton();
        bind(Random.class).annotatedWith(Names.named("pseudo")).toInstance(new Random());
        bind(EventBusBuilder.class).annotatedWith(Names.named("player")).to(EventBusBuilder.class);
    }

    @Provides
    @Singleton
    @Named("player")
    Experience providePlayerExperience(Config config) {
        Path path = Paths.get(config.getString("heat.world.experience-table-path"));
        return Experiences.read(path, Experiences.PLAYER_COLUMN);
    }

    @Provides
    @Singleton
    PlayerRegistry providePlayerRegistry(EventBusBuilder eventBusBuilder) {
        return new DefaultPlayerRegistry(eventBusBuilder.build());
    }
}
