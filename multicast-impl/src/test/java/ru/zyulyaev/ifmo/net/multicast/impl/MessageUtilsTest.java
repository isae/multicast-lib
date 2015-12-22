package ru.zyulyaev.ifmo.net.multicast.impl;

import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;

/**
 * @author zyulyaev
 * @since 22.12.15
 */
public class MessageUtilsTest {
    @Test
    public void testFormatFeedMessage() {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        FeedMessage message = new FeedMessage("123", new byte[]{1, 2, 3});
        MessageUtils.format(message, buffer);
        buffer.flip();
        Assert.assertEquals(message, MessageUtils.parse(buffer));
    }
}
