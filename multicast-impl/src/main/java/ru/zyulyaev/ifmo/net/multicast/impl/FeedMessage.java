package ru.zyulyaev.ifmo.net.multicast.impl;

/**
 * Created by nikita on 20.12.15.
 */
public class FeedMessage implements Message {
    private final String id;
    private final byte[] data;

    public FeedMessage(String id, byte[] data) {
        this.id = id;
        this.data = data;
    }

    public String getId() {
        return id;
    }

    public byte[] getData() {
        return data;
    }

    @Override
    public byte getType() {
        return FEED_MESSAGE;
    }
}
