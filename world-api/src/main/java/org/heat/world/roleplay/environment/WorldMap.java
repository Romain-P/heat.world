package org.heat.world.roleplay.environment;

import com.ankamagames.dofus.network.enums.DirectionsEnum;
import com.github.blackrush.acara.EventBus;
import com.google.common.collect.Sets;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.heat.dofus.data.MapData;
import org.heat.world.roleplay.WorldActor;
import org.heat.world.roleplay.environment.events.ActorEntranceEvent;
import org.heat.world.roleplay.environment.events.ActorMovementEvent;
import org.heat.world.roleplay.environment.events.ActorRefreshEvent;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static com.ankamagames.dofus.network.enums.DirectionsEnum.*;
import static java.util.Objects.requireNonNull;

@RequiredArgsConstructor
public final class WorldMap {
    @Getter final EventBus eventBus;
    @Getter final MapData data;
    final Set<WorldActor> actors = Sets.newConcurrentHashSet();

    public int getId() {
        return (int) data.getId();
    }

    public Stream<WorldActor> getActorStream() {
        return actors.stream();
    }

    /**
     * Add an actor to the map
     * @param actor a non-null actor
     * @throws java.lang.IllegalArgumentException if the map already contains the actor
     * @throws java.lang.NullPointerException if the parameter is null
     */
    public void addActor(WorldActor actor) {
        requireNonNull(actor, "actor");
        if (actors.contains(actor)) {
            throw new IllegalArgumentException();
        }

        actors.add(actor);
        eventBus.publish(new ActorEntranceEvent(this, actor, true));
    }

    /**
     * Remove an actor from the map
     * @param actor a non-null actor
     * @throws java.lang.IllegalArgumentException if the map doesn't contain the actor
     * @throws java.lang.NullPointerException if the parameter is null
     */
    public void removeActor(WorldActor actor) {
        requireNonNull(actor, "actor");
        if (!actors.remove(actor)) {
            throw new IllegalArgumentException();
        }
        eventBus.publish(new ActorEntranceEvent(this, actor, false));
    }

    /**
     * Refresh an actor on the map
     * @param actor a non-null actor
     * @throws java.lang.IllegalArgumentException if the map already contains the actor
     * @throws java.lang.NullPointerException if the parameter is null
     */
    public void refreshActor(WorldActor actor) {
        requireNonNull(actor, "actor");
        if (!actors.contains(actor)) {
            throw new IllegalArgumentException();
        }

        eventBus.publish(new ActorRefreshEvent(this, actor));
    }

    /**
     * Move an actor on the map
     * @param actor a non-null actor
     * @param path a non-null path
     * @throws java.lang.IllegalArgumentException if the map already contains the actor
     * @throws java.lang.NullPointerException if a parameter is null
     */
    public void moveActor(WorldActor actor, WorldMapPath path) {
        requireNonNull(actor, "actor");
        requireNonNull(path, "path");
        if (!actors.contains(actor)) {
            throw new IllegalArgumentException();
        }

        eventBus.publish(new ActorMovementEvent(this, actor, path));
    }

    public Optional<DirectionsEnum> tryOrientationTo(WorldMap other) {
        if (this.data.getTopNeighbourId() == other.data.getId()) {
            return Optional.of(DIRECTION_NORTH);
        } else if (this.data.getRightNeighbourId() == other.data.getId()) {
            return Optional.of(DIRECTION_EAST);
        } else if (this.data.getBottomNeighbourId() == other.data.getId()) {
            return Optional.of(DIRECTION_SOUTH);
        } else if (this.data.getLeftNeighbourId() == other.data.getId()) {
            return Optional.of(DIRECTION_WEST);
        }

        return Optional.empty();
    }

    public boolean isAdjacentTo(WorldMap other) {
        return tryOrientationTo(other).isPresent();
    }

    public DirectionsEnum orientationTo(WorldMap other) {
        return tryOrientationTo(other)
                .orElseThrow(() -> new IllegalArgumentException(String.format(
                        "map %d isn't adjacent to %d",
                        this.data.getId(), other.data.getId())))
                ;
    }

    @Override
    public String toString() {
        return "WorldMap(" +
                "id=" + getId() +
                ", nr-actors=" + actors.size() +
                ")";
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this;
    }

    @Override
    public int hashCode() {
        long id = data.getId();
        return (int) (id ^ (id >>> 32));
    }
}
