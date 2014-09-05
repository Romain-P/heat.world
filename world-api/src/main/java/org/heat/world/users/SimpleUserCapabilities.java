package org.heat.world.users;

import com.ankamagames.dofus.datacenter.breeds.Breed;
import com.google.common.collect.ImmutableList;
import org.heat.User;
import org.heat.datacenter.Datacenter;
import org.rocket.Service;
import org.rocket.ServiceContext;

import javax.inject.Singleton;
import java.util.List;
import java.util.Optional;

@Singleton
public class SimpleUserCapabilities implements UserCapabilities, Service {
    private List<Breed> breeds;

    @Override
    public Optional<Class<? extends Service>> dependsOn() {
        return Optional.of(Datacenter.class);
    }

    @Override
    public void start(ServiceContext ctx) {
        Datacenter datacenter = ctx.getInjector().getInstance(Datacenter.class);
        this.breeds = ImmutableList.copyOf(datacenter.findAll(Breed.class).get().values());
    }

    @Override
    public void stop(ServiceContext ctx) { }

    @Override
    public boolean isTutorialAvailable(User user) {
        return true;
    }

    @Override
    public List<Breed> getVisibleBreeds(User user) {
        return breeds;
    }

    @Override
    public List<Breed> getAvailableBreeds(User user) {
        return breeds;
    }
}
