package org.heat.world;

import com.github.blackrush.acara.supervisor.Supervisor;
import com.github.blackrush.acara.supervisor.SupervisorDirective;

public class StdWorldSupervisor implements Supervisor {
    @Override
    public SupervisorDirective handle(Throwable cause) {
        return SupervisorDirective.IGNORE;
    }
}
