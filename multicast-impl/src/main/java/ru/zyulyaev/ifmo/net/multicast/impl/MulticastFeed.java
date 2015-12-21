package ru.zyulyaev.ifmo.net.multicast.impl;

import ru.zyulyaev.ifmo.net.multicast.api.Feed;

import java.net.InetAddress;

/**
 * Created by nikita on 19.12.15.
 */
public class MulticastFeed implements Feed {
    private final InetAddress group;
    private final String id;
    private final String topic;
    private final String description;

    public MulticastFeed(InetAddress group, String id, String topic, String description) {
        this.group = group;
        this.id = id;
        this.topic = topic;
        this.description = description;
    }

    @Override
    public String getTopic() {
        return topic;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public InetAddress getGroup() {
        return group;
    }

    public String getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MulticastFeed that = (MulticastFeed) o;

        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "MulticastFeed{" +
                "group=" + group +
                ", id='" + id + '\'' +
                ", topic='" + topic + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
