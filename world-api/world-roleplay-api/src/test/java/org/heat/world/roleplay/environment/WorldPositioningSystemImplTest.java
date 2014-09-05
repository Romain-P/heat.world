package org.heat.world.roleplay.environment;

import com.ankamagames.dofus.network.enums.DirectionsEnum;
import com.github.blackrush.acara.EventBus;
import com.github.blackrush.acara.EventBusBuilder;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.*;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.netty.buffer.UnpooledByteBufAllocator;
import org.fungsi.concurrent.Workers;
import org.heat.datacenter.Datacenter;
import org.heat.datacenter.FileDatacenter;
import org.heat.datacenter.FileMapDatacenter;
import org.heat.datacenter.MapDatacenter;
import org.heat.dofus.d2o.D2oReader;
import org.heat.dofus.d2o.HeatDataClassLookup;
import org.heat.dofus.d2p.adapters.MapsAdapter;
import org.heat.shared.IntPair;
import org.heat.world.StdWorldEnvironmentModule;
import org.junit.Before;
import org.junit.Test;
import org.rocket.ImmutableServiceContext;

import javax.inject.Inject;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WorldPositioningSystemImplTest {

    @Inject Datacenter datacenter;
    @Inject WorldPositioningSystemImpl wps;
    @Inject EventBusBuilder eventBusBuilder;

    @Before
    public void setUp() throws Exception {
        File configFile = new File("world.conf").getAbsoluteFile();

        if (!configFile.exists()) {
            throw new FileNotFoundException(configFile.toString());
        }

        Config config = ConfigFactory.parseFileAnySyntax(configFile);

        Injector injector = Guice.createInjector(
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        install(new StdWorldEnvironmentModule());

                        bind(Config.class).toInstance(config);
                        bind(EventBusBuilder.class).toInstance(mock(EventBusBuilder.class));
                        bind(ExecutorService.class).toInstance(MoreExecutors.sameThreadExecutor());
                    }

                    @Provides
                    @Singleton
                    Datacenter provideDatacenter() {
                        return new FileDatacenter(
                                Paths.get(config.getString("heat.world.datacenter-path")),
                                new D2oReader(new UnpooledByteBufAllocator(false), HeatDataClassLookup.INSTANCE)
                        );
                    }

                    @Provides
                    @Singleton
                    MapDatacenter provideMapDatacenter() throws IOException {
                        return new FileMapDatacenter(
                                Workers.wrap(MoreExecutors.sameThreadExecutor()),
                                MapsAdapter.withDefault(config.getString("heat.world.maps.key")),
                                new UnpooledByteBufAllocator(false),
                                Files.list(Paths.get(config.getString("heat.world.maps.data-path")))
                                    .filter(x -> x.getFileName().toString().endsWith(".d2p"))
                        );
                    }
                }
        );
        injector.injectMembers(this);

        ImmutableServiceContext ctx = ImmutableServiceContext.of(config, ClassLoader.getSystemClassLoader(), injector);
        datacenter.start(ctx);
        wps.start(ctx);

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