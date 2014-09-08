package org.heat.world.users;

import lombok.*;
import org.heat.UserRank;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalInt;

@Getter
@Setter
@EqualsAndHashCode
@ToString
@RequiredArgsConstructor
public final class WorldUser {
    int id;
    org.heat.User user;

    //<editor-fold desc="org.heat.User delegate">
    public double getSubscriptionEndMilliOrZero() {
        return user.getSubscriptionEndMilliOrZero();
    }

    public String getHashpass() {
        return user.getHashpass();
    }

    public String getUsername() {
        return user.getUsername();
    }

    public String getSalt() {
        return user.getSalt();
    }

    public String getNickname() {
        return user.getNickname();
    }

    public String getSecretQuestion() {
        return user.getSecretQuestion();
    }

    public boolean isConnected() {
        return user.isConnected();
    }

    public String getSecretAnswer() {
        return user.getSecretAnswer();
    }

    public Optional<Instant> getBanEnd() {
        return user.getBanEnd();
    }

    public UserRank getRank() {
        return user.getRank();
    }

    public Instant getCreatedAt() {
        return user.getCreatedAt();
    }

    public OptionalInt getCurrentWorldId() {
        return user.getCurrentWorldId();
    }

    public Instant getUpdatedAt() {
        return user.getUpdatedAt();
    }

    public Optional<Instant> getSubscriptionEnd() {
        return user.getSubscriptionEnd();
    }

    public OptionalInt getLastServerId() {
        return user.getLastServerId();
    }

    public byte getCommunityId() {
        return user.getCommunityId();
    }
    //</editor-fold>
}
