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
import org.apache.rocketmq.common.BrokerConfig;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.message.MessageExtBatch;
import org.apache.rocketmq.store.config.MessageStoreConfig;
import org.apache.rocketmq.store.config.StorePathConfigHelper;
import org.apache.rocketmq.store.stats.BrokerStatsManager;

import static org.apache.rocketmq.store.config.BrokerRole.SLAVE;

public class NonPersistentMessageStore extends DefaultMessageStore {

    public NonPersistentMessageStore(MessageStoreConfig messageStoreConfig,
        BrokerStatsManager brokerStatsManager,
        MessageArrivingListener messageArrivingListener,
        BrokerConfig brokerConfig) throws IOException {
        super(messageStoreConfig, brokerStatsManager, messageArrivingListener, brokerConfig);
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
        return super.putMessage(msg);
    }
    public GetMessageResult getMessage(final String group, final String topic, final int queueId, final long offset,
        final int maxMsgNums,
        final MessageFilter messageFilter){
        return super.getMessage(group,topic,queueId,offset,maxMsgNums,messageFilter);
    }
    public long getMaxOffsetInQueue(String topic, int queueId) {
        return super.getMaxOffsetInQueue(topic,queueId);
    }

    public long getMinOffsetInQueue(String topic, int queueId) {
        return super.getMinOffsetInQueue(topic,queueId);
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
