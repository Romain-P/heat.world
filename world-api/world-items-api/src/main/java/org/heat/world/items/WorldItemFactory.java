package org.heat.world.items;

import com.ankamagames.dofus.datacenter.items.Item;

public interface WorldItemFactory {
    /**
     * Create an item given a template and a quantity
     * @param template a non-null item
     * @param quantity an integer
     * @return a non-null item
     */
    WorldItem create(Item template, int quantity);
}
