package org.heat.world.chat;

public interface PrivateChannelMessage extends WorldChannelMessage {
    WorldChannelMessage getMessage();

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

        public ByReceiverId(WorldChannelMessage message, int receiverId) {
            super(message);
            this.receiverId = receiverId;
        }

        public int getReceiverId() {
            return receiverId;
        }
    }

    public static final class ByReceiverName extends Base {
        private final String receiverName;

        public ByReceiverName(WorldChannelMessage message, String receiverName) {
            super(message);
            this.receiverName = receiverName;
        }

        public String getReceiverName() {
            return receiverName;
        }
    }

}
