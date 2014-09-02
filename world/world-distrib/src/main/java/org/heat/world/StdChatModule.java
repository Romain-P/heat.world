package org.heat.world;

import com.github.blackrush.acara.EventBusBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.heat.world.chat.SharedChannelLookup;
import org.heat.world.chat.WorldChannelLookup;
import org.heat.world.players.PlayerRegistry;
import org.heat.world.players.chat.VirtualPrivateChannelLookup;

import java.time.Clock;

public class StdChatModule extends AbstractModule {
    // NOTE(Blackrush):
    //  i decided to put the module here since multiple implementations
    //  are split across differents modules

    @Override
    protected void configure() {

    }

    @Provides
    @Singleton
    WorldChannelLookup provideChannelLookup(PlayerRegistry playerRegistry, EventBusBuilder eventBusBuilder, Clock clock) {
        return new VirtualPrivateChannelLookup(
                playerRegistry,
                new SharedChannelLookup(eventBusBuilder, clock)
        );
    }
}
