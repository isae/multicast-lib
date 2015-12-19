package ru.zyulyaev.ifmo.net.multicast.api;

/**
 * Created by nikita on 19.12.15.
 */
public interface Subscription {
    Feed getFeed();

    void unsubscribe();
}
