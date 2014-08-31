package org.heat.world.controllers;

import com.ankamagames.dofus.network.messages.game.chat.ChatClientMultiMessage;
import com.ankamagames.dofus.network.messages.game.chat.ChatClientMultiWithObjectMessage;
import com.ankamagames.dofus.network.messages.game.chat.ChatClientPrivateMessage;
import com.ankamagames.dofus.network.messages.game.chat.ChatClientPrivateWithObjectMessage;
import com.github.blackrush.acara.Listener;
import org.heat.shared.stream.ImmutableCollectors;
import org.heat.world.chat.*;
import org.heat.world.controllers.events.ChoosePlayerEvent;
import org.heat.world.controllers.utils.RolePlaying;
import org.heat.world.players.Player;
import org.rocket.network.Controller;
import org.rocket.network.NetworkClient;
import org.rocket.network.Prop;
import org.rocket.network.Receive;

import javax.inject.Inject;
import java.util.stream.Stream;

@Controller
@RolePlaying
public class ChatController {
    @Inject NetworkClient client;
    @Inject Prop<Player> player;

    @Inject WorldChannelLookup channelLookup;

    private void doSpeak(WorldChannelMessage message) {
        WorldChannel channel = channelLookup.lookupChannel(message);
        channel.speak(player.get(), message);
    }

    @Listener
    public void listenPrivateMessages(ChoosePlayerEvent evt) {
        evt.getPlayer().getEventBus().subscribe(this);
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
}
