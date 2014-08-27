package org.heat.world.backend;

import com.ankamagames.dofus.network.enums.ServerStatusEnum;
import com.google.common.collect.Maps;
import com.google.inject.name.Named;
import com.typesafe.config.Config;
import org.fungsi.concurrent.Future;
import org.fungsi.concurrent.Futures;
import org.fungsi.concurrent.Timer;
import org.heat.User;
import org.heat.backend.UserAlreadyConnectedException;
import org.heat.backend.messages.AckUserAuthReq;
import org.heat.backend.messages.SetNrPlayersReq;
import org.heat.backend.messages.SetStatusReq;
import org.heat.shared.Strings;
import org.rocket.network.NetworkClient;
import org.rocket.network.NetworkClientService;

import javax.inject.Inject;
import java.time.Duration;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public final class DefaultBackend implements Backend {

    private final NetworkClient client;
    private final Random random;
    private final Timer userAuthTtl;
    private final Duration userAuthTtlDuration;
    private final Map<String, User> users = Maps.newConcurrentMap();

    private ServerStatusEnum status = ServerStatusEnum.ONLINE;

    @Inject
    public DefaultBackend(
            @Named("backend") NetworkClientService client,
            @Named("ticket") Random random,
            @Named("user-auth-ttl") Timer userAuthTtl,
            Config config
    ) {
        this.client = client;
        this.random = random;
        this.userAuthTtl = userAuthTtl;
        this.userAuthTtlDuration = Duration.ofNanos(config.getDuration("heat.world.backend.user-auth-ttl", TimeUnit.NANOSECONDS));
    }

    @Override
    public ServerStatusEnum getCurrentStatus() {
        return status;
    }

    @Override
    public Future<String> authorizeUser(User user) {
        if (users.containsValue(user)) {
            return Futures.failure(new UserAlreadyConnectedException());
        }
        String ticket = Strings.randomString(random, 64);
        users.put(ticket, user);
        userAuthTtl.schedule(userAuthTtlDuration, () -> {
            users.remove(ticket);
            client.write(new AckUserAuthReq(user.getId(), false));
        });
        return Futures.success(ticket);
    }

    @Override
    public Future<User> authenticateUser(String ticket) {
        User user = users.remove(ticket);

        if (user == null) {
            return Futures.failure(new NoSuchElementException());
        }

        client.write(new AckUserAuthReq(user.getId(), true));
        return Futures.success(user);
    }

    @Override
    public void setNewStatus(ServerStatusEnum newStatus) {
        client.write(new SetStatusReq(newStatus));
    }

    @Override
    public void setNrPlayers(int userId, int nrPlayers) {
        client.write(new SetNrPlayersReq(userId, nrPlayers));
    }
}
