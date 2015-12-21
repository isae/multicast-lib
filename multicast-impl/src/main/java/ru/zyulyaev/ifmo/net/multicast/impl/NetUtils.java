package ru.zyulyaev.ifmo.net.multicast.impl;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by nikita on 19.12.15.
 */
public class NetUtils {
    private static final int FIRST_GROUP = 0xE0000000;
    private static final int LAST_GROUP = 0xEFFFFFFF;

    /** <CODE>224.0.0.0</CODE> */
    public static InetAddress getHeartbeatGroup() {
        return getGroupByIndex(FIRST_GROUP);
    }

    /** <CODE>224.0.0.1</CODE> to <CODE>239.255.255.255</CODE> */
    public static InetAddress getRandomFeedGroup()  {
        return getGroupByIndex(ThreadLocalRandom.current().nextInt(FIRST_GROUP, LAST_GROUP) + 1);
    }

    private static InetAddress getGroupByIndex(int group) {
        byte[] addr = new byte[]{(byte) (group >> 24), (byte) (group >> 16), (byte) (group >> 8), (byte) group};
        try {
            return InetAddress.getByAddress(addr);
        } catch (UnknownHostException e) {
            // wtf?!
            throw new AssertionError(e);
        }
    }
}
