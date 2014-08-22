package org.heat.world.roleplay.environment;

import com.ankamagames.dofus.network.enums.DirectionsEnum;
import com.github.blackrush.acara.EventBus;
import com.google.common.collect.Sets;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.heat.dofus.data.MapData;
import org.heat.world.items.WorldItem;
import org.heat.world.roleplay.WorldActor;
import org.heat.world.roleplay.environment.events.*;

import java.util.HashMap;
import java.util.Map;
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
    final Map<WorldMapPoint, WorldItem> items = new HashMap<>();

    public int getId() {
        return (int) data.getId();
    }

    public Stream<WorldActor> getActorStream() {
        return actors.stream();
    }

    // Map.Entry is mutable, do we need to return a copy instead?
    public Stream<Map.Entry<WorldMapPoint, WorldItem>> getItems() {
        return items.entrySet().stream();
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

    /**
     * Determine whether or not this map has an actor on the given map point
     * @param mapPoint a non-null map point
     * @return {@code true} if it has an actor on the given map point, {@code false} otherwise
     */
    public boolean hasActorOn(WorldMapPoint mapPoint) {
        return actors.stream().anyMatch(x -> x.getActorPosition().getMapPoint().equals(mapPoint));
    }

    /**
     * Add an item on the map
     * @param item a non-null item
     * @param mapPoint a non-null map point
     * @param exact if {@code false} will search first available adjacents cells
     * @return {@code true} there was enough room, {@code false} otherwise
     * @throws java.lang.IllegalArgumentException if item already has been added
     */
    public boolean addItem(WorldItem item, WorldMapPoint mapPoint, boolean exact) {
        requireNonNull(item, "item");
        requireNonNull(mapPoint, "mapPoint");

        if (items.containsValue(item)) {
            throw new IllegalArgumentException("map already contains " + item);
        }

        synchronized (items) {
            if (exact) {
                if (items.containsKey(mapPoint)) {
                    return false;
                }

                items.put(mapPoint, item);
            } else {
                Optional<WorldMapPoint> option =
                    Stream.concat(Stream.of(mapPoint), mapPoint.adjacents(true))
                        .filter(x -> !hasActorOn(x) && items.containsKey(x))
                        .findAny();

                if (!option.isPresent()) {
                    return false;
                }

                WorldMapPoint firstAvailable = option.get();
                items.put(firstAvailable, item);
            }
        }

        eventBus.publish(new MapItemAddEvent(this, item, mapPoint));
        return true;
    }

    /**
     * Add an item on the map on the exact given map point
     * @param item a non-null item
     * @param mapPoint a non-null map point
     * @return {@code true} there was enough room, {@code false} otherwise
     * @throws java.lang.IllegalArgumentException if item already has been added
     */
    public boolean addItem(WorldItem item, WorldMapPoint mapPoint) {
        return addItem(item, mapPoint, true);
    }

    /**
     * Remove an item on the map given a map point
     * @param mapPoint a non-null map point
     * @return an optional item
     */
    public Optional<WorldItem> removeItem(WorldMapPoint mapPoint) {
        requireNonNull(mapPoint, "mapPoint");

        synchronized (items) {
            WorldItem item;
            item = items.remove(mapPoint);
            if (item == null) {
                return Optional.empty();
            }
            eventBus.publish(new MapItemRemoveEvent(this, item, mapPoint));
            return Optional.of(item);
        }
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
