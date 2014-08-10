package org.heat.world.users;

import com.ankamagames.dofus.datacenter.breeds.Breed;
import com.google.common.collect.ImmutableList;
import org.heat.User;
import org.heat.data.Datacenter;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

public class SimpleUserCapabilities implements UserCapabilities {
    private final List<Breed> breeds;

    @Inject
    public SimpleUserCapabilities(Datacenter datacenter) {
        Map<Integer, Breed> breeds = datacenter.findAll(Breed.class).get();
        this.breeds = ImmutableList.copyOf(breeds.values());
    }

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
