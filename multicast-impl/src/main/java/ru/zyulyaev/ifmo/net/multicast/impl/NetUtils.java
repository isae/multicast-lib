package ru.zyulyaev.ifmo.net.multicast.impl;

import com.google.common.net.InetAddresses;

import java.net.InetAddress;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by nikita on 19.12.15.
 */
public class NetUtils {
    private static final int FIRST_GROUP = 0xE0000000;
    private static final int LAST_GROUP = 0xEFFFFFFF;

    /** <CODE>224.0.0.0</CODE> */
    public static InetAddress getHeartbeatGroup() {
        return InetAddresses.fromInteger(FIRST_GROUP);
    }

    /** <CODE>224.0.0.1</CODE> to <CODE>239.255.255.255</CODE> */
    public static InetAddress getRandomFeedGroup()  {
        return InetAddresses.fromInteger(ThreadLocalRandom.current().nextInt(FIRST_GROUP, LAST_GROUP) + 1);
    }
}
