/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.rocketmq.store;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.apache.rocketmq.common.BrokerConfig;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.message.MessageExtBatch;
import org.apache.rocketmq.store.config.BrokerRole;
import org.apache.rocketmq.store.config.MessageStoreConfig;
import org.apache.rocketmq.store.config.StorePathConfigHelper;
import org.apache.rocketmq.store.stats.BrokerStatsManager;

import static org.apache.rocketmq.store.config.BrokerRole.SLAVE;

public class NonPersistentMessageStore extends DefaultMessageStore {

    private ConcurrentMap<String/* topic */, ConcurrentMap<Integer/* queueId */, LinkedList<MessageExtBrokerInner>>> store;

    public NonPersistentMessageStore(MessageStoreConfig messageStoreConfig,
        BrokerStatsManager brokerStatsManager,
        MessageArrivingListener messageArrivingListener,
        BrokerConfig brokerConfig) throws IOException {
        super(messageStoreConfig, brokerStatsManager, messageArrivingListener, brokerConfig);

        store = new ConcurrentHashMap<String,ConcurrentMap<Integer/* queueId */, LinkedList<MessageExtBrokerInner>>>();
    }
    /**
     * @throws Exception
     */
    public void start() throws Exception {
        super.start();
    }

    public void shutdown() {
        super.shutdown();
    }
    public PutMessageResult putMessage(MessageExtBrokerInner msg){
        if (this.shutdown) {
            log.warn("message store has shutdown, so putMessage is forbidden");
            return new PutMessageResult(PutMessageStatus.SERVICE_NOT_AVAILABLE, null);
        }

        if (BrokerRole.SLAVE == this.messageStoreConfig.getBrokerRole()) {
            long value = this.printTimes.getAndIncrement();
            if ((value % 50000) == 0) {
                log.warn("message store is slave mode, so putMessage is forbidden ");
            }

            return new PutMessageResult(PutMessageStatus.SERVICE_NOT_AVAILABLE, null);
        }
        if (!this.runningFlags.isWriteable()) {
            long value = this.printTimes.getAndIncrement();
            if ((value % 50000) == 0) {
                log.warn("message store is not writeable, so putMessage is forbidden " + this.runningFlags.getFlagBits());
            }

            return new PutMessageResult(PutMessageStatus.SERVICE_NOT_AVAILABLE, null);
        } else {
            this.printTimes.set(0);
        }

        if (msg.getTopic().length() > Byte.MAX_VALUE) {
            log.warn("putMessage message topic length too long " + msg.getTopic().length());
            return new PutMessageResult(PutMessageStatus.MESSAGE_ILLEGAL, null);
        }

        if (msg.getPropertiesString() != null && msg.getPropertiesString().length() > Short.MAX_VALUE) {
            log.warn("putMessage message properties length too long " + msg.getPropertiesString().length());
            return new PutMessageResult(PutMessageStatus.PROPERTIES_SIZE_EXCEEDED, null);
        }

        if (this.isOSPageCacheBusy()) {
            return new PutMessageResult(PutMessageStatus.OS_PAGECACHE_BUSY, null);
        }

        long beginTime = this.getSystemClock().now();
        if(store.get(msg.getTopic())==null){
            ConcurrentHashMap<Integer,LinkedList<MessageExtBrokerInner>> map = new ConcurrentHashMap<Integer,LinkedList<MessageExtBrokerInner>>();
            store.put(msg.getTopic(),map);
            if(map.get(msg.getQueueId())==null){
                LinkedList<MessageExtBrokerInner> list = new LinkedList<MessageExtBrokerInner>();
                list.offer(msg);
                map.put(msg.getQueueId(),list);
            }
            else{
                map.get(msg.getQueueId()).offer(msg);
            }
        }else {
            if(store.get(msg.getTopic()).get(msg.getQueueId())==null){
                LinkedList<MessageExtBrokerInner> list = new LinkedList<MessageExtBrokerInner>();
                list.offer(msg);
                store.get(msg.getTopic()).put(msg.getQueueId(),list);
            }else{
                store.get(msg.getTopic()).get(msg.getQueueId()).offer(msg);
            }

        }
        long eclipseTime = this.getSystemClock().now() - beginTime;
        if (eclipseTime > 500) {
            log.warn("putMessage not in lock eclipse time(ms)={}, bodyLength={}", eclipseTime, msg.getBody().length);
        }
        this.storeStatsService.setPutMessageEntireTimeMax(eclipseTime);

        return new PutMessageResult(PutMessageStatus.PUT_OK, null);
    }
    public GetMessageResult getMessage(final String group, final String topic, final int queueId, final long offset,
        final int maxMsgNums,
        final MessageFilter messageFilter){

        if (this.shutdown) {
            log.warn("message store has shutdown, so getMessage is forbidden");
            return null;
        }

        if (!this.runningFlags.isReadable()) {
            log.warn("message store is not readable, so getMessage is forbidden " + this.runningFlags.getFlagBits());
            return null;
        }

        long beginTime = this.getSystemClock().now();

        GetMessageStatus status = GetMessageStatus.NO_MESSAGE_IN_QUEUE;
        long nextBeginOffset = 0;
        long minOffset = 0;
        long maxOffset = 0;
        GetMessageResult getResult = new GetMessageResult();

        LinkedList<MessageExtBrokerInner> msg =  store.get(topic).get(queueId);
        LinkedList<MessageExtBrokerInner> result = null;
        if(msg!=null){
            if(msg.size()==0){
                status = GetMessageStatus.NO_MESSAGE_IN_QUEUE;
            }
            if(msg.size()<=maxMsgNums){
                status = GetMessageStatus.FOUND;
                result = new LinkedList<MessageExtBrokerInner>(msg);
            }
            if(msg.size()>maxMsgNums){
                status = GetMessageStatus.FOUND;
                result = new LinkedList<MessageExtBrokerInner>();
                for(int i=0;i<maxMsgNums;i++){
                    result.offer(msg.poll());
                }
                maxOffset = msg.size() - maxMsgNums;
            }


        }else {
            status = GetMessageStatus.NO_MATCHED_LOGIC_QUEUE;
        }

        if (GetMessageStatus.FOUND == status) {
            this.storeStatsService.getGetMessageTimesTotalFound().incrementAndGet();
        } else {
            this.storeStatsService.getGetMessageTimesTotalMiss().incrementAndGet();
        }
        long eclipseTime = this.getSystemClock().now() - beginTime;
        this.storeStatsService.setGetMessageEntireTimeMax(eclipseTime);
        getResult.addAllMessage(result);
        getResult.setStatus(status);
        getResult.setNextBeginOffset(nextBeginOffset);
        getResult.setMaxOffset(maxOffset);
        getResult.setMinOffset(minOffset);

        return getResult;


    }
    public long getMaxOffsetInQueue(String topic, int queueId) {
        return store.get(topic).get(queueId).size();
    }

    public long getMinOffsetInQueue(String topic, int queueId) {
        return 0;
    }

    @Override
    public long getCommitLogOffsetInQueue(String topic, int queueId, long consumeQueueOffset) {
        return super.getCommitLogOffsetInQueue(topic,queueId,consumeQueueOffset);
    }


    public MessageExt lookMessageByOffset(long commitLogOffset) {
        return  super.lookMessageByOffset(commitLogOffset);
    }

    public SelectMappedBufferResult getCommitLogData(final long offset) {
        return super.getCommitLogData(offset);
    }

}
