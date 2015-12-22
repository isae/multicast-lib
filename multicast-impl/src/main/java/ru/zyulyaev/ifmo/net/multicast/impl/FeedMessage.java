package ru.zyulyaev.ifmo.net.multicast.impl;

import java.util.Arrays;

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


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FeedMessage that = (FeedMessage) o;

        return id.equals(that.id) && Arrays.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + Arrays.hashCode(data);
        return result;
    }

    @Override
    public String toString() {
        return "FeedMessage{" +
                "id='" + id + '\'' +
                ", data=" + Arrays.toString(data) +
                '}';
    }
}
