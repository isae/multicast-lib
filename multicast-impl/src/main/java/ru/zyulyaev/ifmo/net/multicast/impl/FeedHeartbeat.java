package ru.zyulyaev.ifmo.net.multicast.impl;

import java.util.Arrays;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FeedHeartbeat that = (FeedHeartbeat) o;

        if (!id.equals(that.id)) return false;
        if (!Arrays.equals(groupAddress, that.groupAddress)) return false;
        if (!topic.equals(that.topic)) return false;
        return description.equals(that.description);
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + Arrays.hashCode(groupAddress);
        result = 31 * result + topic.hashCode();
        result = 31 * result + description.hashCode();
        return result;
    }


    @Override
    public String toString() {
        return "FeedHeartbeat{" +
                "id='" + id + '\'' +
                ", groupAddress=" + Arrays.toString(groupAddress) +
                ", topic='" + topic + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
