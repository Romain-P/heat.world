package org.heat.world;

import com.google.inject.CreationException;
import com.google.inject.ProvisionException;
import com.google.inject.spi.Message;
import org.rocket.dist.RocketLauncher;

public final class App {
    private App() {}

    public static void main(String[] args) {
        try {
            RocketLauncher.run(new StdWorldRocket().getServiceContext());
        } catch (CreationException e) {
            for (Message message : e.getErrorMessages()) {
                message.getCause().printStackTrace();
            }
        } catch (ProvisionException e) {
            for (Message message : e.getErrorMessages()) {
                message.getCause().printStackTrace();
            }
        }
    }
}
