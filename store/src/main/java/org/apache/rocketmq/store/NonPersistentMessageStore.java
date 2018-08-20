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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.message.MessageExtBatch;

public class NonPersistentMessageStore implements MessageStore {
    public boolean load() {
        return false;
    }

    public void start() throws Exception {

    }

    public void shutdown() {

    }

    public void destroy() {

    }

    public PutMessageResult putMessage(MessageExtBrokerInner msg) {
        return null;
    }

    public PutMessageResult putMessages(MessageExtBatch messageExtBatch) {
        return null;
    }

    public GetMessageResult getMessage(String group, String topic, int queueId, long offset,
        int maxMsgNums, MessageFilter messageFilter) {
        return null;
    }

    public long getMaxOffsetInQueue(String topic, int queueId) {
        return 0;
    }

    public long getMinOffsetInQueue(String topic, int queueId) {
        return 0;
    }

    public long getCommitLogOffsetInQueue(String topic, int queueId, long consumeQueueOffset) {
        return 0;
    }

    public long getOffsetInQueueByTime(String topic, int queueId, long timestamp) {
        return 0;
    }

    public MessageExt lookMessageByOffset(long commitLogOffset) {
        return null;
    }

    public SelectMappedBufferResult selectOneMessageByOffset(long commitLogOffset) {
        return null;
    }

    public SelectMappedBufferResult selectOneMessageByOffset(long commitLogOffset, int msgSize) {
        return null;
    }

    public String getRunningDataInfo() {
        return null;
    }

    public HashMap<String, String> getRuntimeInfo() {
        return null;
    }

    public long getMaxPhyOffset() {
        return 0;
    }

    public long getMinPhyOffset() {
        return 0;
    }

    public long getEarliestMessageTime(String topic, int queueId) {
        return 0;
    }

    public long getEarliestMessageTime() {
        return 0;
    }

    public long getMessageStoreTimeStamp(String topic, int queueId, long consumeQueueOffset) {
        return 0;
    }

    public long getMessageTotalInQueue(String topic, int queueId) {
        return 0;
    }

    public SelectMappedBufferResult getCommitLogData(long offset) {
        return null;
    }

    public boolean appendToCommitLog(long startOffset, byte[] data) {
        return false;
    }

    public void executeDeleteFilesManually() {

    }

    public QueryMessageResult queryMessage(String topic, String key, int maxNum, long begin,
        long end) {
        return null;
    }

    public void updateHaMasterAddress(String newAddr) {

    }

    public long slaveFallBehindMuch() {
        return 0;
    }

    public long now() {
        return 0;
    }

    public int cleanUnusedTopic(Set<String> topics) {
        return 0;
    }

    public void cleanExpiredConsumerQueue() {

    }

    public boolean checkInDiskByConsumeOffset(String topic, int queueId, long consumeOffset) {
        return false;
    }

    public long dispatchBehindBytes() {
        return 0;
    }

    public long flush() {
        return 0;
    }

    public boolean resetWriteOffset(long phyOffset) {
        return false;
    }

    public long getConfirmOffset() {
        return 0;
    }

    public void setConfirmOffset(long phyOffset) {

    }

    public boolean isOSPageCacheBusy() {
        return false;
    }

    public long lockTimeMills() {
        return 0;
    }

    public boolean isTransientStorePoolDeficient() {
        return false;
    }

    public LinkedList<CommitLogDispatcher> getDispatcherList() {
        return null;
    }

    public ConsumeQueue getConsumeQueue(String topic, int queueId) {
        return null;
    }
}
