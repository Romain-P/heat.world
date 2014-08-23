package org.heat.world;

import com.ankamagames.dofus.network.ProtocolTypeManager;
import com.ankamagames.dofus.network.types.game.data.items.effects.ObjectEffect;
import com.google.inject.PrivateModule;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import org.fungsi.concurrent.Worker;
import org.fungsi.concurrent.Workers;
import org.heat.dofus.network.NetworkComponentFactory;
import org.heat.shared.database.Table;
import org.heat.world.items.*;

import java.util.concurrent.ExecutorService;

public class StdItemsModule extends PrivateModule {
    @Override
    protected void configure() {
        bind(new TypeLiteral<Table<WorldItem>>() {}).to(WorldItemTable.class);
        bind(WorldItemFactory.class).to(DefaultItemFactory.class).asEagerSingleton();
        bind(WorldItemRepository.class).annotatedWith(Names.named("base")).to(JdbcItemRepository.class).asEagerSingleton();
        bind(WorldItemRepository.class).to(PermLazyItemRepository.class).asEagerSingleton();

        expose(WorldItemFactory.class);
        expose(WorldItemRepository.class);
    }

    @SuppressWarnings("unchecked")
    @Provides
    NetworkComponentFactory<ObjectEffect> provideObjectEffectFactory() {
        return (NetworkComponentFactory) ProtocolTypeManager.createNewManager();
    }

    @Provides
    Worker provideItemRepositoryWorker(ExecutorService executor) {
        return Workers.wrap(executor);
    }
}
