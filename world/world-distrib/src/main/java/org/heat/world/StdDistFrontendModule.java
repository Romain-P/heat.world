package org.heat.world;

import com.ankamagames.dofus.network.MessageReceiver;
import com.github.blackrush.acara.EventBusBuilder;
import com.google.inject.*;
import com.google.inject.name.Named;
import com.typesafe.config.Config;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import org.heat.dofus.network.NetworkComponentFactory;
import org.heat.dofus.network.NetworkMessage;
import org.heat.dofus.network.netty.DofusDecoder;
import org.heat.dofus.network.netty.DofusEncoder;
import org.rocket.network.ControllerFactory;
import org.rocket.network.NetworkService;
import org.rocket.network.guice.ControllerFactoryModule;
import org.rocket.network.netty.RocketNetty;
import org.slf4j.LoggerFactory;

public class StdDistFrontendModule extends PrivateModule {
    private final Module controllersModule;

    public StdDistFrontendModule(Module controllersModule) {
        this.controllersModule = controllersModule;
    }

    @Override
    protected void configure() {
        install(controllersModule);
        install(new ControllerFactoryModule());
    }

    @Provides
    NetworkComponentFactory<NetworkMessage> provideMessageFactory() {
        return MessageReceiver.DEFAULT;
    }

    @Provides
    @Exposed
    @Singleton
    @Named("frontend")
    NetworkService provideNetworkService(
            EventBusBuilder eventBusBuilder,
            ControllerFactory controllerFactory,
            NetworkComponentFactory<NetworkMessage> messageFactory,
            Config config
    ) {
        return RocketNetty.newService(
                eventBusBuilder::build,
                controllerFactory,
                bootstrap -> {
                    bootstrap.localAddress(config.getInt("heat.world.frontend.port"));
                    bootstrap.channelFactory(NioServerSocketChannel::new);
                },
                pipeline -> {
                    pipeline.addLast("decoder", new DofusDecoder(messageFactory));
                    pipeline.addLast("encoder", new DofusEncoder());

                    pipeline.addLast("logging", new LoggingHandler("frontend-network"));
                },
                LoggerFactory.getLogger("frontend-network")
        );
    }
}
