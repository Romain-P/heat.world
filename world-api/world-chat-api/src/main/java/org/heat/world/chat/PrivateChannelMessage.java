package org.heat.world.chat;

public interface PrivateChannelMessage extends WorldChannelMessage {
    public static final int CHANNEL_ID = -1;

    WorldChannelMessage getMessage();

    @Override
    default int getChannelId() {
        return CHANNEL_ID;
    }

    @Override
    default String getString() {
        return getMessage().getString();
    }

    static abstract class Base implements PrivateChannelMessage {
        private final WorldChannelMessage message;

        protected Base(WorldChannelMessage message) {
            this.message = message;
        }

        @Override
        public WorldChannelMessage getMessage() {
            return message;
        }
    }

    public static final class ByReceiverId extends Base {
        private final int receiverId;

        public ByReceiverId(int receiverId, WorldChannelMessage message) {
            super(message);
            this.receiverId = receiverId;
        }

        public int getReceiverId() {
            return receiverId;
        }
    }

    public static final class ByReceiverName extends Base {
        private final String receiverName;

        public ByReceiverName(String receiverName, WorldChannelMessage message) {
            super(message);
            this.receiverName = receiverName;
        }

        public String getReceiverName() {
            return receiverName;
        }
    }

}
