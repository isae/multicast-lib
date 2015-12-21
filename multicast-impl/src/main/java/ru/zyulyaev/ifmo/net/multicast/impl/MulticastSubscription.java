package ru.zyulyaev.ifmo.net.multicast.impl;

import ru.zyulyaev.ifmo.net.multicast.api.Feed;
import ru.zyulyaev.ifmo.net.multicast.api.FeedListener;
import ru.zyulyaev.ifmo.net.multicast.api.Subscription;

import java.nio.channels.MembershipKey;

/**
 * Created by nikita on 20.12.15.
 */
public class MulticastSubscription implements Subscription {
    private final Feed feed;
    private final MembershipKey membershipKey;
    private final FeedListener listener;

    public MulticastSubscription(Feed feed, MembershipKey membershipKey, FeedListener listener) {
        this.feed = feed;
        this.membershipKey = membershipKey;
        this.listener = listener;
    }

    @Override
    public Feed getFeed() {
        return feed;
    }

    @Override
    public void unsubscribe() {
        membershipKey.drop();
    }

    public FeedListener getListener() {
        return listener;
    }
}
