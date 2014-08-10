package org.heat.world.roleplay.environment;

import com.ankamagames.dofus.network.enums.DirectionsEnum;
import com.github.blackrush.acara.EventBus;
import com.github.blackrush.acara.EventBusBuilder;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.Guice;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.heat.StdDataModule;
import org.heat.shared.IntPair;
import org.heat.world.StdWorldEnvironmentModule;
import org.junit.Before;
import org.junit.Test;

import javax.inject.Inject;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.concurrent.ExecutorService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WorldPositioningSystemImplTest {

    @Inject WorldPositioningSystem wps;
    @Inject EventBusBuilder eventBusBuilder;

    @Before
    public void setUp() throws Exception {
        File configFile = new File(System.getenv("PWD") + "/world.conf").getAbsoluteFile();

        if (!configFile.exists()) {
            throw new FileNotFoundException(configFile.toString());
        }

        Guice.createInjector(
                new StdDataModule(),
                new StdWorldEnvironmentModule(),
                binder -> {
                    binder.bind(Config.class).toInstance(ConfigFactory.parseFileAnySyntax(configFile));
                    binder.bind(EventBusBuilder.class).toInstance(mock(EventBusBuilder.class));
                    binder.bind(ExecutorService.class).toInstance(MoreExecutors.sameThreadExecutor());
                }
        ).injectMembers(this);

        when(eventBusBuilder.build()).thenReturn(mock(EventBus.class));
    }

    @Test
    public void testLocate() throws Exception {
        // given
        int map = 16395;
        WorldMapPoint mapPoint = WorldMapPoint.of(355).get();
        DirectionsEnum direction = DirectionsEnum.DIRECTION_SOUTH_EAST;

        // when
        FullWorldPosition pos = (FullWorldPosition) wps.locate(map, mapPoint, direction);

        // then
        assertNotNull("position", pos);
        assertEquals("position map id", map, pos.getMapId());
        assertEquals("position mapPoint", mapPoint, pos.getMapPoint());
        assertEquals("position direction", direction, pos.getDirection());
        assertEquals("position coordinates", IntPair.of(32, 11), pos.getMapCoordinates());
    }
}