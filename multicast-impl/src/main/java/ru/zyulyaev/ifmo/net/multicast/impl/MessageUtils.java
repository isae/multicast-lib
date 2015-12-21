package ru.zyulyaev.ifmo.net.multicast.impl;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * Created by nikita on 20.12.15.
 */
public class MessageUtils {
    public static final Charset CHARSET = Charset.forName("UTF-8");

    public static Message parse(ByteBuffer buffer) {
        switch (buffer.get()) {
            case Message.FEED_HEARTBEAT:
                return parseFeedHeartbeat(buffer);
            case Message.FEED_MESSAGE:
                return parseFeedMessage(buffer);
        }
        return null;
    }

    private static FeedHeartbeat parseFeedHeartbeat(ByteBuffer buffer) {
        String id = getString(buffer);
        byte[] address = new byte[4];
        buffer.get(address);
        String topic = getString(buffer);
        String description = getString(buffer);
        return new FeedHeartbeat(id, address, topic, description);
    }

    private static FeedMessage parseFeedMessage(ByteBuffer buffer) {
        String id = getString(buffer);
        byte[] data = new byte[buffer.remaining()];
        buffer.get(data);
        return new FeedMessage(id, data);
    }

    public static ByteBuffer format(FeedHeartbeat heartbeat, ByteBuffer buffer) {
        buffer.put(heartbeat.getType());
        putString(buffer, heartbeat.getId());
        buffer.put(heartbeat.getGroupAddress());
        putString(buffer, heartbeat.getTopic());
        putString(buffer, heartbeat.getDescription());
        return buffer;
    }

    public static ByteBuffer format(FeedMessage message, ByteBuffer buffer) {
        buffer.put(message.getType());
        putString(buffer, message.getId());
        return buffer.put(message.getData());
    }

    private static void putString(ByteBuffer buffer, String value) {
        buffer.put(value.getBytes(CHARSET))
                .put((byte) 0);
    }

    private static String getString(ByteBuffer buffer) {
        buffer.mark();
        int length = 0;
        while (buffer.get() != 0)
            length++;
        buffer.reset();
        byte[] bytes = new byte[length];
        buffer.get(bytes);
        return new String(bytes, CHARSET);
    }

    private MessageUtils() {
    }
}
