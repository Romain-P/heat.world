package org.heat.world.players.items;

import org.heat.world.items.WorldItem;
import org.heat.world.items.WorldItemWallet;

import java.util.stream.Stream;

import static com.ankamagames.dofus.network.enums.CharacterInventoryPositionEnum.INVENTORY_POSITION_NOT_EQUIPED;

public interface PlayerItemWallet extends WorldItemWallet {
    WorldItemWallet createTemp();

    default int getWeight() {
        return getItemStream()
                .mapToInt(item -> (int) item.getTemplate().getRealWeight())
                .reduce(0, Integer::sum);
    }

    default Stream<WorldItem> getEquipedStream() {
        return getItemStream().filter(x -> x.getPosition() != INVENTORY_POSITION_NOT_EQUIPED);
    }
}
