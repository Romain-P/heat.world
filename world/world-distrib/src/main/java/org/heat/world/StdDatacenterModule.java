package org.heat.world;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.typesafe.config.Config;
import io.netty.buffer.ByteBufAllocator;
import org.fungsi.concurrent.Workers;
import org.heat.datacenter.Datacenter;
import org.heat.datacenter.FileDatacenter;
import org.heat.datacenter.FileMapDatacenter;
import org.heat.datacenter.MapDatacenter;
import org.heat.dofus.d2o.D2oReader;
import org.heat.dofus.d2o.HeatDataClassLookup;
import org.heat.dofus.d2p.adapters.MapsAdapter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;

public class StdDatacenterModule extends AbstractModule {
    @Override
    protected void configure() {

    }

    @Provides
    D2oReader provideD2oReader(ByteBufAllocator alloc) {
        return new D2oReader(alloc, HeatDataClassLookup.INSTANCE);
    }

    @Provides
    MapsAdapter provideMapsAdapter(Config config) {
        return MapsAdapter.withDefault(config.getString("heat.world.maps.key"));
    }

    @Provides
    @Singleton
    Datacenter provideDatacenter(Config config, D2oReader reader) {
        return new FileDatacenter(Paths.get(config.getString("heat.world.datacenter-path")), reader);
    }

    @Provides
    @Singleton
    MapDatacenter provideMapDatacenter(ExecutorService executor, MapsAdapter adapter, ByteBufAllocator alloc, Config config) throws IOException {
        Stream<Path> paths = Files.list(Paths.get(config.getString("heat.world.maps.data-path")))
                .filter(x -> x.getFileName().toString().endsWith(".d2p"));

        return new FileMapDatacenter(Workers.wrap(executor), adapter, alloc, paths);
    }
}
