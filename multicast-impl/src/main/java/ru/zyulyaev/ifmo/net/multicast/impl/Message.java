package ru.zyulyaev.ifmo.net.multicast.impl;

/**
 * Created by nikita on 20.12.15.
 */
public interface Message {
    byte FEED_HEARTBEAT = 0;
    byte FEED_MESSAGE = 1;

    byte getType();

    default FeedHeartbeat asFeedHeartbeat() {
        return (FeedHeartbeat) this;
    }

    default FeedMessage asFeedMessage() {
        return (FeedMessage) this;
    }
}
