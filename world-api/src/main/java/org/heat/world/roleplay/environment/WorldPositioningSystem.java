package org.heat.world.roleplay.environment;

import com.ankamagames.dofus.network.enums.DirectionsEnum;

public interface WorldPositioningSystem {
    WorldPosition locate(int mapId, WorldMapPoint cellId, DirectionsEnum dir);
    WorldPosition resolve(WorldPosition position);
}
