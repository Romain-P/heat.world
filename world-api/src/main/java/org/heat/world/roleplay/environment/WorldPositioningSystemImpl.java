package org.heat.world.roleplay.environment;

import com.ankamagames.dofus.datacenter.world.Area;
import com.ankamagames.dofus.datacenter.world.MapPosition;
import com.ankamagames.dofus.datacenter.world.SubArea;
import com.ankamagames.dofus.datacenter.world.SuperArea;
import com.ankamagames.dofus.network.enums.DirectionsEnum;
import com.google.common.collect.Maps;
import org.heat.data.Datacenter;
import org.heat.shared.IntPair;
import org.heat.shared.database.Repository;

import javax.inject.Inject;
import java.time.Duration;
import java.util.Map;

public class WorldPositioningSystemImpl implements WorldPositioningSystem {
    private final Datacenter datacenter;
    private final Repository<WorldMap> maps;
    private final Map<Integer, MapPosition> positions;

    @Inject
    public WorldPositioningSystemImpl(Datacenter datacenter, Repository<WorldMap> maps) {
        this.datacenter = datacenter;
        this.maps = maps;
        this.positions = Maps.newHashMap();

        for (MapPosition position : datacenter.findAll(MapPosition.class).get(Duration.ofMillis(500)).values()) {
            this.positions.put(position.getId(), position);
        }
    }

    @Override
    public WorldPosition locate(int mapId, WorldMapPoint mapPoint, DirectionsEnum dir) {
        MapPosition pos = positions.get(mapId);
        if (pos == null) {
            throw new UnresolvableWorldPositionException();
        }

        WorldMap map = maps.find(mapId).get();
        SubArea subArea = datacenter.find(SubArea.class, map.getData().getSubareaId()).get();
        Area area = datacenter.find(Area.class, subArea.getAreaId()).get();
        SuperArea superArea = datacenter.find(SuperArea.class, area.getSuperAreaId()).get();

        return new FullWorldPosition(this, superArea, area, subArea, map, IntPair.of(pos.getPosX(), pos.getPosY()), mapPoint, dir);
    }

    @Override
    public WorldPosition resolve(WorldPosition position) {
        if (position instanceof UnresolvedWorldPosition) {
            return locate(position.getMapId(), position.getMapPoint(), position.getDirection());
        }

        return position;
    }
}
