package org.heat.world.roleplay.environment;

import com.ankamagames.dofus.datacenter.world.Area;
import com.ankamagames.dofus.datacenter.world.SubArea;
import com.ankamagames.dofus.datacenter.world.SuperArea;
import com.ankamagames.dofus.network.enums.DirectionsEnum;
import com.ankamagames.dofus.network.types.game.context.EntityDispositionInformations;
import org.heat.dofus.d2p.data.MapData;
import org.heat.shared.IntPair;

public interface WorldPosition {
    boolean isResolved();

    int getSuperAreaId();
    SuperArea getSuperArea();

    int getAreaId();
    Area getArea();

    int getSubAreaId();
    SubArea getSubArea();

    int getMapId();
    IntPair getMapCoordinates();
    WorldMap getMap();

    WorldMapPoint getMapPoint();
    DirectionsEnum getDirection();

    WorldPosition moveTo(int mapId, WorldMapPoint point, DirectionsEnum dir);

    default MapData getMapData() {
        return getMap().getData();
    }

    default short getCellId() {
        return getMapPoint().cellId;
    }

    default EntityDispositionInformations toEntityDispositionInformations() {
        return new EntityDispositionInformations(getCellId(), getDirection().value);
    }

    default WorldPosition moveTo(WorldMapPoint point, DirectionsEnum dir) {
        return moveTo(getMapId(), point, dir);
    }

    default WorldPosition goToTop() {
        return moveTo(
                (int) getMapData().getTopNeighbourId(),
                getMapPoint().plusCellId(532),
                DirectionsEnum.DIRECTION_NORTH
        );
    }

    default WorldPosition goToRight() {
        return moveTo(
                (int) getMapData().getRightNeighbourId(),
                getMapPoint().plusCellId(-13),
                DirectionsEnum.DIRECTION_EAST
        );
    }

    default WorldPosition goToBottom() {
        return moveTo(
                (int) getMapData().getBottomNeighbourId(),
                getMapPoint().plusCellId(-532),
                DirectionsEnum.DIRECTION_SOUTH
        );
    }

    default WorldPosition goToLeft() {
        return moveTo(
                (int) getMapData().getLeftNeighbourId(),
                getMapPoint().plusCellId(13),
                DirectionsEnum.DIRECTION_WEST
        );
    }

    default WorldPosition goToMap(int mapId) {
        MapData data = getMapData();

        if      (mapId == data.getTopNeighbourId())     return goToTop();
        else if (mapId == data.getRightNeighbourId())   return goToRight();
        else if (mapId == data.getBottomNeighbourId())  return goToBottom();
        else if (mapId == data.getLeftNeighbourId())    return goToLeft();

        throw new InvalidPositionNavigationException();
    }
}
