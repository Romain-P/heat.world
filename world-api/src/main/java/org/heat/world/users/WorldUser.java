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
    final org.heat.User user;

    //<editor-fold desc="org.heat.User delegate">
    public double getSubscriptionEndMilliOrZero() {
        return user.getSubscriptionEndMilliOrZero();
    }

    public void setCommunityId(byte communityId) {
        user.setCommunityId(communityId);
    }

    public void setSecretAnswer(String secretAnswer) {
        user.setSecretAnswer(secretAnswer);
    }

    public void setBanEnd(Optional<Instant> banEnd) {
        user.setBanEnd(banEnd);
    }

    public String getHashpass() {
        return user.getHashpass();
    }

    public void setSecretQuestion(String secretQuestion) {
        user.setSecretQuestion(secretQuestion);
    }

    public String getUsername() {
        return user.getUsername();
    }

    public void setId(int id) {
        user.setId(id);
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

    public void setCreatedAt(Instant createdAt) {
        user.setCreatedAt(createdAt);
    }

    public void setCurrentWorldId(OptionalInt currentWorldId) {
        user.setCurrentWorldId(currentWorldId);
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

    public void setHashpass(String hashpass) {
        user.setHashpass(hashpass);
    }

    public void setSubscriptionEnd(Optional<Instant> subscriptionEnd) {
        user.setSubscriptionEnd(subscriptionEnd);
    }

    public void setConnected(boolean connected) {
        user.setConnected(connected);
    }

    public Instant getCreatedAt() {
        return user.getCreatedAt();
    }

    public OptionalInt getCurrentWorldId() {
        return user.getCurrentWorldId();
    }

    public void setRank(UserRank rank) {
        user.setRank(rank);
    }

    public Instant getUpdatedAt() {
        return user.getUpdatedAt();
    }

    public void setUsername(String username) {
        user.setUsername(username);
    }

    public Optional<Instant> getSubscriptionEnd() {
        return user.getSubscriptionEnd();
    }

    public int getId() {
        return user.getId();
    }

    public void setSalt(String salt) {
        user.setSalt(salt);
    }

    public void setNickname(String nickname) {
        user.setNickname(nickname);
    }

    public OptionalInt getLastServerId() {
        return user.getLastServerId();
    }

    public void setUpdatedAt(Instant updatedAt) {
        user.setUpdatedAt(updatedAt);
    }

    public void setLastServerId(OptionalInt lastServerId) {
        user.setLastServerId(lastServerId);
    }

    public byte getCommunityId() {
        return user.getCommunityId();
    }
    //</editor-fold>
}
