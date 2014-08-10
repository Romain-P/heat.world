package org.heat.world;

import com.google.inject.AbstractModule;
import org.heat.world.users.SimpleUserCapabilities;
import org.heat.world.users.UserCapabilities;

public class StdUsersModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(UserCapabilities.class).to(SimpleUserCapabilities.class).asEagerSingleton();
    }
}
