package org.heat.world.items;

public class OutdatedItemException extends RuntimeException {
    private final WorldItem outdated;
    private final WorldItem updated;

    public OutdatedItemException(WorldItem outdated, WorldItem updated) {
        super("an item is outdated");
        this.outdated = outdated;
        this.updated = updated;
    }

    public WorldItem getOutdated() {
        return outdated;
    }

    public WorldItem getUpdated() {
        return updated;
    }
}
