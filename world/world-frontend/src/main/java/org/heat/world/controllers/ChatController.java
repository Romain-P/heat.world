package org.heat.world.controllers;

import com.ankamagames.dofus.network.messages.game.chat.*;
import com.ankamagames.dofus.network.messages.game.chat.channel.EnabledChannelsMessage;
import com.github.blackrush.acara.Listener;
import lombok.extern.slf4j.Slf4j;
import org.heat.shared.stream.ImmutableCollectors;
import org.heat.world.chat.*;
import org.heat.world.controllers.events.ChoosePlayerEvent;
import org.heat.world.controllers.events.EnterContextEvent;
import org.heat.world.controllers.events.QuitContextEvent;
import org.heat.world.controllers.utils.Basics;
import org.heat.world.controllers.utils.RolePlaying;
import org.heat.world.items.WorldItem;
import org.heat.world.players.Player;
import org.heat.world.users.WorldUser;
import org.rocket.network.Controller;
import org.rocket.network.NetworkClient;
import org.rocket.network.Prop;
import org.rocket.network.Receive;

import javax.inject.Inject;
import java.util.stream.Stream;

import static com.ankamagames.dofus.network.enums.ChatActivableChannelsEnum.PSEUDO_CHANNEL_PRIVATE;

@Controller
@RolePlaying
@Slf4j
public class ChatController {
    @Inject NetworkClient client;
    @Inject Prop<Player> player;
    @Inject Prop<WorldUser> user;

    @Inject WorldChannelLookup channelLookup;

    private void doSpeak(WorldChannelMessage message) {
        if (!user.get().hasChannel(message.getChannelId())) {
            client.write(Basics.noop());
            return;
        }

        WorldChannel channel = channelLookup.lookupChannel(message);

        if (channel != null) {
            channel.speak(player.get(), message);
        } else {
            log.debug("cannot speak on channel {}", message.getChannelId());
            client.write(Basics.noop());
        }
    }

    private void subscribeAllChannels() {
        WorldUser user = this.user.get();

        channelLookup.forEach(channel -> {
            if (user.hasChannel(channel.getChannelId())) {
                channel.getSubscribableChannelView().subscribe(this);
            }
        });
    }

    private void unsubscribeAllChannels() {
        WorldUser user = this.user.get();

        channelLookup.forEach(channel -> {
            if (user.hasChannel(channel.getChannelId())) {
                channel.getSubscribableChannelView().unsubscribe(this);
            }
        });
    }

    @Listener
    public void sendEnabledChannels(ChoosePlayerEvent evt) {
        client.write(new EnabledChannelsMessage(
                evt.getPlayer().getUser().getChannelsAsBytes(),
                evt.getPlayer().getUser().getDisabledChannelsAsBytes()
        ));
    }

    @Listener
    public void subscribeChannels(EnterContextEvent evt) {
        subscribeAllChannels();
    }

    @Listener
    public void unsubscribeChannels(QuitContextEvent evt) {
        unsubscribeAllChannels();
    }

    @Receive
    public void speak(ChatClientMultiMessage msg) {
        WorldChannelMessage message = new StringChannelMessage(msg.channel, msg.content);

        doSpeak(message);
    }

    @Receive
    public void speakWithAttachments(ChatClientMultiWithObjectMessage msg) {
        Player player = this.player.get();

        WorldChannelMessage message = new ChannelMessageWithAttachments(
            new StringChannelMessage(msg.channel, msg.content),
            Stream.of(msg.objects)
                .map(item -> player.getWallet().findByUid(item.objectUID).get())
                .collect(ImmutableCollectors.toList()));

        doSpeak(message);
    }

    @Receive
    public void privatelySpeak(ChatClientPrivateMessage msg) {
        WorldChannelMessage message = new PrivateChannelMessage.ByReceiverName(
                msg.receiver,
                new StringChannelMessage(PrivateChannelMessage.CHANNEL_ID, msg.content));

        doSpeak(message);
    }

    @Receive
    public void privatelySpeakWithAttachments(ChatClientPrivateWithObjectMessage msg) {
        Player player = this.player.get();

        WorldChannelMessage message = new PrivateChannelMessage.ByReceiverName(
            msg.receiver,
            new ChannelMessageWithAttachments(
                new StringChannelMessage(PrivateChannelMessage.CHANNEL_ID, msg.content),
                Stream.of(msg.objects)
                    .map(item -> player.getWallet().findByUid(item.objectUID).get())
                    .collect(ImmutableCollectors.toList())));

        doSpeak(message);
    }

    @Listener
    public void onEnvelope(WorldChannelEnvelope envelope) {
        Player player = this.player.get();

        WorldSpeaker speaker = envelope.getSpeaker();

        if (envelope.getMessage() instanceof PrivateChannelMessage && speaker == player) {
            PrivateChannelMessage privateMessage = (PrivateChannelMessage) envelope.getMessage();
            WorldMessageReceiver receiver = privateMessage.getReceiver();
            WorldChannelMessage message = privateMessage.getMessage();

            if (message instanceof ChannelMessageWithAttachments) {
                client.write(new ChatServerCopyWithObjectMessage(
                        PSEUDO_CHANNEL_PRIVATE.value,
                        message.getString(),
                        (int) envelope.getInstant().getEpochSecond(),
                        "",
                        receiver.getSpeakerId(),
                        receiver.getSpeakerName(),
                        ((ChannelMessageWithAttachments) message).getAttachments().stream()
                            .map(WorldItem::toObjectItem)
                ));
            } else {
                client.write(new ChatServerCopyMessage(
                        PSEUDO_CHANNEL_PRIVATE.value,
                        message.getString(),
                        (int) envelope.getInstant().getEpochSecond(),
                        "",
                        receiver.getSpeakerId(),
                        receiver.getSpeakerName()
                ));
            }
        } else {
            WorldChannelMessage message = envelope.getMessage();

            if (message instanceof ChannelMessageWithAttachments) {
                client.write(new ChatServerWithObjectMessage(
                        (byte) message.getChannelId(),
                        message.getString(),
                        (int) envelope.getInstant().getEpochSecond(),
                        "",
                        speaker.getSpeakerId(),
                        speaker.getSpeakerName(),
                        speaker.getSpeakerUserId(),
                        ((ChannelMessageWithAttachments) message).getAttachments().stream()
                            .map(WorldItem::toObjectItem)
                ));
            } else {
                client.write(new ChatServerMessage(
                        (byte) message.getChannelId(),
                        message.getString(),
                        (int) envelope.getInstant().getEpochSecond(),
                        "",
                        speaker.getSpeakerId(),
                        speaker.getSpeakerName(),
                        speaker.getSpeakerUserId()
                ));
            }
        }
    }
}
