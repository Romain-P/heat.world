package org.heat.world.backend;

import com.ankamagames.dofus.network.enums.ServerStatusEnum;
import org.fungsi.concurrent.Future;
import org.heat.User;

public interface Backend {
    ServerStatusEnum getCurrentStatus();

    Future<String> authorizeUser(User user);
    Future<User> authenticateUser(String ticket);

    void setNewStatus(ServerStatusEnum newStatus);
    void setNrPlayers(int userId, int nrPlayers);
}
