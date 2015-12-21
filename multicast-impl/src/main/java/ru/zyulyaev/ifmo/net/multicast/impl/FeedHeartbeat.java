package ru.zyulyaev.ifmo.net.multicast.impl;

/**
 * Created by nikita on 20.12.15.
 */
public class FeedHeartbeat implements Message {
    private final String id;
    private final byte[] groupAddress;
    private final String topic;
    private final String description;

    public FeedHeartbeat(String id, byte[] groupAddress, String topic, String description) {
        this.id = id;
        this.groupAddress = groupAddress;
        this.topic = topic;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public byte[] getGroupAddress() {
        return groupAddress;
    }

    public String getTopic() {
        return topic;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public byte getType() {
        return FEED_HEARTBEAT;
    }
}
