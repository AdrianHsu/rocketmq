package org.apache.rocketmq.store;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.rocketmq.common.BrokerConfig;
import org.apache.rocketmq.store.config.FlushDiskType;
import org.apache.rocketmq.store.config.MessageStoreConfig;
import org.apache.rocketmq.store.stats.BrokerStatsManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class NonPersistentMessageStoreTest {


    private final String StoreMessage = "Once, there was a chance for me!";
    private int QUEUE_TOTAL = 100;
    private AtomicInteger QueueId = new AtomicInteger(0);
    private SocketAddress BornHost;
    private SocketAddress StoreHost;
    private byte[] MessageBody = StoreMessage.getBytes();
    private MessageStore messageStore;

    @Before
    public void init() throws Exception {
        StoreHost = new InetSocketAddress(InetAddress.getLocalHost(), 8123);
        BornHost = new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0);

        messageStore = buildMessageStore();
        messageStore.start();
    }

    private MessageStore buildMessageStore() throws Exception {
        MessageStoreConfig messageStoreConfig = new MessageStoreConfig();
        messageStoreConfig.setMapedFileSizeCommitLog(1024 * 1024 * 10);
        messageStoreConfig.setMapedFileSizeConsumeQueue(1024 * 1024 * 10);
        messageStoreConfig.setMaxHashSlotNum(10000);
        messageStoreConfig.setMaxIndexNum(100 * 100);
        messageStoreConfig.setFlushDiskType(FlushDiskType.SYNC_FLUSH);
        messageStoreConfig.setFlushIntervalConsumeQueue(1);
        return new NonPersistentMessageStore(messageStoreConfig, new BrokerStatsManager("simpleTest"), new MyMessageArrivingListener(), new BrokerConfig());
    }

    /** Unit test for put message and get message */
    @Test
    public void testWriteAndRead() {
        QUEUE_TOTAL = 1;
        MessageExtBrokerInner msg = buildMessage();

        // print message body
        System.out.println(msg.getBody());

        // put message body
        messageStore.putMessage(msg);

        // get message
        GetMessageResult result = messageStore.getMessage("GROUP_A", "FooBar", 0, 0, 1024 * 1024, null);
        assertThat(result).isNotNull();

        // print get message body
        System.out.println(result.getResult().get(0).getBody());

        Assert.assertEquals(msg.getBody(), result.getResult().get(0).getBody());

        result.release();
    }

    private MessageExtBrokerInner buildMessage() {
        MessageExtBrokerInner msg = new MessageExtBrokerInner();
        msg.setTopic("FooBar");
        msg.setTags("TAG1");
        msg.setKeys("Hello");
        msg.setBody(MessageBody);
        msg.setKeys(String.valueOf(System.currentTimeMillis()));
        msg.setQueueId(Math.abs(QueueId.getAndIncrement()) % QUEUE_TOTAL);
        msg.setSysFlag(0);
        msg.setBornTimestamp(System.currentTimeMillis());
        msg.setStoreHost(StoreHost);
        msg.setBornHost(BornHost);
        return msg;
    }

    private class MyMessageArrivingListener implements MessageArrivingListener {
        @Override
        public void arriving(String topic, int queueId, long logicOffset, long tagsCode, long msgStoreTime,
            byte[] filterBitMap, Map<String, String> properties) {
        }
    }

}
