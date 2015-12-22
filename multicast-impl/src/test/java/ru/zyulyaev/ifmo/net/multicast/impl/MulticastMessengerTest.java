package ru.zyulyaev.ifmo.net.multicast.impl;

import org.junit.Assert;
import org.junit.Test;
import ru.zyulyaev.ifmo.net.multicast.api.Feed;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.hamcrest.CoreMatchers.hasItem;

/**
 * @author zyulyaev
 * @since 22.12.15
 */
public class MulticastMessengerTest {
    private static final int PORT = 4000;

    @Test
    public void testRegisterFeed() throws Exception {
        try (MulticastMessenger messenger = MulticastMessenger.open(PORT)) {
            Feed feed = messenger.registerFeed("Topic", "Description").get();
            Thread.sleep(100);
            Assert.assertThat(messenger.discoverFeeds().get(), hasItem(feed));
        }
    }

    @Test
    public void testSendMessage() throws Exception {
        try (MulticastMessenger messenger = MulticastMessenger.open(PORT)) {
            Feed feed = messenger.registerFeed("Topic", "Description").get();
            Queue<byte[]> incomingMessages = new ConcurrentLinkedQueue<>();
            messenger.subscribe(feed, (message, subscription) -> incomingMessages.add(message)).get();
            byte[] message = {1, 2, 3};
            messenger.sendMessage(feed, message);
            Thread.sleep(100);
            Assert.assertArrayEquals(message, incomingMessages.poll());
            Assert.assertTrue(incomingMessages.isEmpty());
        }
    }
}
