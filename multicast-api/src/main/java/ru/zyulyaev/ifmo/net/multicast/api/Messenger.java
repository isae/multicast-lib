package ru.zyulyaev.ifmo.net.multicast.api;

import java.util.Collection;
import java.util.concurrent.Future;

/**
 * Created by nikita on 19.12.15.
 */
public interface Messenger {
    Future<Feed> registerFeed(String topic, String description);

    Future<Collection<Feed>> discoverFeeds();

    Future<Subscription> subscribe(Feed feed, FeedListener listener);

    void sendMessage(Feed feed, byte[] message) throws Exception;
}
