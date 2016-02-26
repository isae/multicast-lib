package ru.zyulyaev.ifmo.net.multicast.impl;

import com.google.common.io.Closer;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.zyulyaev.ifmo.net.multicast.api.Feed;
import ru.zyulyaev.ifmo.net.multicast.api.FeedListener;
import ru.zyulyaev.ifmo.net.multicast.api.Messenger;
import ru.zyulyaev.ifmo.net.multicast.api.Subscription;

import java.io.Closeable;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.MembershipKey;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.*;
import java.util.concurrent.*;


/**
 * Created by nikita on 19.12.15.
 */
public class MulticastMessenger implements Messenger, Closeable {
    private static final Logger log = LoggerFactory.getLogger(MulticastMessenger.class);

    private static final int BUFFER_SIZE = 4 * 1024 * 1024; // 4MB

    private static final int HEARTBEAT_DELAY = 1000;

    private final ExecutorService messagesService = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
            .setNameFormat("MulticastMessenger-Messages-%d")
            .build());

    private final Map<String, MulticastFeed> knownFeeds = new ConcurrentHashMap<>();

    private final Set<MulticastFeed> localFeeds = new CopyOnWriteArraySet<>();

    private final Map<String, List<MulticastSubscription>> subscriptionsMap = new ConcurrentHashMap<>();

    private final Deque<Message> messageQueue = new ArrayDeque<>();

    private final Thread backgroundThread;

    private final Thread broadcastThread;

    private final Selector selector;

    private final DatagramChannel listenerChannel;

    private final SelectionKey listenerKey;

    private final int port;

    private final ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);

    private MulticastMessenger(Selector selector, DatagramChannel listenerChannel, int port) throws IOException {
        this.selector = selector;
        this.listenerChannel = listenerChannel;
        this.port = port;

        listenerKey = listenerChannel.register(selector, SelectionKey.OP_READ, (Processor) this::processKey);

        backgroundThread = new Thread(this::mainLoop, "MutlicastMessenger-MainLoop");
        backgroundThread.start();

        broadcastThread = new Thread(this::broadcastLoop, "MulticastMessenger-Broadcast");
        broadcastThread.start();
    }

    private static NetworkInterface getLocalNetworkInterface() throws UnknownHostException, SocketException {
        NetworkInterface networkInterface = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
        if (networkInterface != null) {
            return networkInterface;
        } else {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface ni = en.nextElement();
                for (Enumeration<InetAddress> enA = ni.getInetAddresses(); enA.hasMoreElements(); ) {
                    InetAddress addr = enA.nextElement();
                    if (addr.isLoopbackAddress()) {
                        return ni;
                    }
                }
            }
            throw new IllegalStateException("Failed to find network interface!");
        }
    }

    public static MulticastMessenger open(int port) throws IOException {
        DatagramChannel listenerChannel = DatagramChannel.open(StandardProtocolFamily.INET);
        listenerChannel.configureBlocking(false);
        InetSocketAddress socketAddress = new InetSocketAddress(port);
        listenerChannel.bind(socketAddress);
        NetworkInterface networkInterface = getLocalNetworkInterface();
        listenerChannel.join(NetUtils.getHeartbeatGroup(), networkInterface);
        return new MulticastMessenger(Selector.open(), listenerChannel, port);
    }

    private static MulticastFeed safeMulticastFeed(Feed feed) {
        if (!(feed instanceof MulticastFeed)) {
            throw new IllegalArgumentException("Unknown feed: " + feed);
        }
        return (MulticastFeed) feed;
    }

    @Override
    public Future<Feed> registerFeed(String topic, String description) {
        String id = UUID.randomUUID().toString();
        InetAddress group = NetUtils.getRandomFeedGroup();
        MulticastFeed feed = new MulticastFeed(group, id, topic, description);
        localFeeds.add(feed);
        enqueueMessage(createHeartbeat(feed));
        return CompletableFuture.completedFuture(feed);
    }

    private static FeedHeartbeat createHeartbeat(MulticastFeed feed) {
        return new FeedHeartbeat(
                feed.getId(),
                feed.getGroup().getAddress(),
                feed.getTopic(),
                feed.getDescription()
        );
    }

    @Override
    public Future<Collection<Feed>> discoverFeeds() {
        return CompletableFuture.completedFuture(new ArrayList<>(knownFeeds.values()));
    }

    @Override
    public Future<Subscription> subscribe(Feed feed, FeedListener listener) {
        MulticastFeed multicastFeed = safeMulticastFeed(feed);
        Objects.requireNonNull(listener, "listener");
        try {
            NetworkInterface networkInterface = getLocalNetworkInterface();
            MembershipKey key = listenerChannel.join(multicastFeed.getGroup(), networkInterface);
            MulticastSubscription subscription = new MulticastSubscription(multicastFeed, key, listener);
            subscriptionsMap.computeIfAbsent(multicastFeed.getId(), ign -> new CopyOnWriteArrayList<>())
                    .add(subscription);
            return CompletableFuture.completedFuture(subscription);
        } catch (Exception e) {
            CompletableFuture<Subscription> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    @Override
    public void sendMessage(Feed feed, byte[] message) throws Exception {
        MulticastFeed multicastFeed = safeMulticastFeed(feed);
        String id = multicastFeed.getId();
        enqueueMessage(new FeedMessage(id, message));
    }


    private synchronized void enqueueMessage(Message message) {
        messageQueue.add(message);
        listenerKey.interestOps(listenerKey.interestOps() | SelectionKey.OP_WRITE);
        selector.wakeup();
    }

    private synchronized void enqueueMessageFirst(Message message) {
        messageQueue.addFirst(message);
        listenerKey.interestOps(listenerKey.interestOps() | SelectionKey.OP_WRITE);
        selector.wakeup();
    }

    private synchronized Message dequeueMessage() {
        Message message = messageQueue.poll();
        if (messageQueue.isEmpty()) {
            listenerKey.interestOps(listenerKey.interestOps() & ~SelectionKey.OP_WRITE);
            selector.wakeup();
        }
        return message;
    }

    private void mainLoop() {
        try {
            while (!Thread.interrupted()) {
                if (selector.select() > 0) {
                    Set<SelectionKey> keys = selector.selectedKeys();
                    for (SelectionKey key : keys) {
                        Processor processor = (Processor) key.attachment();
                        processor.process(key);
                    }
                    keys.clear();
                }
            }
            log.info("Main loop interrupted and exists.");
        } catch (IOException ex) {
            log.error("Main loop exists exceptionally", ex);
        }
    }

    private void processKey(SelectionKey key) throws IOException {
        if (key.isReadable()) {
            processRead(key);
        }
        if (key.isWritable()) {
            processWrite(key);
        }
    }

    private void processRead(SelectionKey key) throws IOException {
        DatagramChannel channel = (DatagramChannel) key.channel();
        while (true) {
            buffer.clear();
            SocketAddress source = channel.receive(buffer);
            if (source == null) {
                break;
            }
            buffer.flip();
            Message message = MessageUtils.parse(buffer);
            if (message == null) {
                log.warn("Unknown message came. Skip.");
                continue;
            }
            log.debug("Received {} from {}.", message, source);
            switch (message.getType()) {
                case Message.FEED_MESSAGE:
                    notifySubscriptions(message.asFeedMessage());
                    break;
                case Message.FEED_HEARTBEAT:
                    updateFeeds(message.asFeedHeartbeat());
                    break;
            }
        }
    }

    private void processWrite(SelectionKey key) throws IOException {
        DatagramChannel channel = (DatagramChannel) key.channel();
        for (Message message; (message = dequeueMessage()) != null; ) {
            buffer.clear();
            InetAddress target = null;
            switch (message.getType()) {
                case Message.FEED_HEARTBEAT:
                    FeedHeartbeat heartbeat = message.asFeedHeartbeat();
                    MessageUtils.format(heartbeat, buffer).flip();
                    target = NetUtils.getHeartbeatGroup();
                    break;
                case Message.FEED_MESSAGE:
                    FeedMessage feedMessage = message.asFeedMessage();
                    MulticastFeed feed = knownFeeds.get(feedMessage.getId());
                    if (feed == null) {
                        log.error("WTF?!");
                        continue;
                    }
                    MessageUtils.format(feedMessage, buffer).flip();
                    target = feed.getGroup();
                    break;
            }
            if (target == null) {
                log.error("Unknown message {}. Skipped.", message);
                continue;
            }
            InetSocketAddress address = new InetSocketAddress(target, port);
            log.debug("Sending {} to {}.", message, address);
            int wrote = channel.send(buffer, address);
            if (wrote == 0) {
                enqueueMessageFirst(message);
                break;
            }
        }
    }

    private void notifySubscriptions(FeedMessage message) {
        List<MulticastSubscription> subscriptions = subscriptionsMap.getOrDefault(message.getId(), Collections.emptyList());
        for (MulticastSubscription subscription : subscriptions) {
            messagesService.submit(() -> subscription.getListener().messageCame(message.getData(), subscription));
        }
    }

    private void updateFeeds(FeedHeartbeat heartbeat) {
        try {
            knownFeeds.put(heartbeat.getId(), new MulticastFeed(
                    InetAddress.getByAddress(heartbeat.getGroupAddress()),
                    heartbeat.getId(),
                    heartbeat.getTopic(),
                    heartbeat.getDescription()
            ));
        } catch (IOException ex) {
            log.error("Failed to update feeds", ex);
        }
    }

    private void broadcastLoop() {
        try {
            while (!Thread.interrupted()) {
                for (MulticastFeed localFeed : localFeeds) {
                    enqueueMessage(createHeartbeat(localFeed));
                }
                Thread.sleep(HEARTBEAT_DELAY);
            }
        } catch (InterruptedException ex) {
            log.info("Thread interrupted");
        }
    }

    @Override
    public void close() throws IOException {
        backgroundThread.interrupt();
        broadcastThread.interrupt();
        try {
            backgroundThread.join(1000);
        } catch (InterruptedException e) {
            log.error("Failed to wait for background thread to stop.", e);
        }
        try {
            broadcastThread.join(1000);
        } catch (InterruptedException e) {
            log.error("Failed to wait for broadcast thread to stop.", e);
        }

        Closer closer = Closer.create();
        for (SelectionKey key : selector.keys()) {
            closer.register(key.channel());
        }
        closer.register(selector);
        closer.close();
    }

    private interface Processor {
        void process(SelectionKey key) throws IOException;
    }
}
