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

package org.apache.rocketmq.example.quickstart;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.MessageExt;

import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;
import java.util.PriorityQueue;

class LabeledMsg {
    public MessageExt msg;
    public int lstId;
    public LabeledMsg(MessageExt msg, int lstId) {
        this.msg = msg;
        this.lstId = lstId;
    }
}
class LabeledMsgComparator implements Comparator<LabeledMsg> {
    public int compare(LabeledMsg l1, LabeledMsg l2) {
        if (l1.msg.getStoreTimestamp() < l2.msg.getStoreTimestamp())
            return 1;
        else if (l1.msg.getStoreTimestamp() < l2.msg.getStoreTimestamp())
            return -1;
        else
            return 0;
    }
}
public class ConsumerOrderly {
    static ArrayList<MessageExt> messageNeedToCompare = new ArrayList<MessageExt>();

    // input: list of mps
    // return: message ready to print
    public static ArrayList<MessageExt> sort(ArrayList<ArrayList<MessageExt>> mps) {
        int sz = mps.size();
        int[] listLen = new int[sz];
        int[] idx = new int[sz];
        ArrayList<MessageExt> ret = new ArrayList<MessageExt>();
        PriorityQueue<LabeledMsg> pq = new PriorityQueue<LabeledMsg>(sz, new LabeledMsgComparator());

        System.out.printf("Start sorting. %n");

        for (int i = 0;i < sz;i++) {
            idx[i] = 1;
            listLen[i] = mps.get(i).size();
            LabeledMsg tmp = new LabeledMsg(mps.get(i).get(0), i);
            pq.add(tmp);
        }

        System.out.printf("Poll first finished. %n");

        while (true) {
            LabeledMsg tmp = pq.poll();
            ret.add(tmp.msg);
            int id = tmp.lstId;
            if (idx[id] >= listLen[id])
                break;
            LabeledMsg addToPq = new LabeledMsg(mps.get(id).get(idx[id]), id);
            idx[id] += 1;
            pq.add(addToPq);
        }

        System.out.printf("Finish while. %n");

        for (int i = 0;i < sz;i++) {
            int curLen = listLen[i];
            for (int j = idx[i];j < curLen;j++) {
                LabeledMsg tmp = new LabeledMsg(mps.get(i).get(j), i);
                pq.add(tmp);
            }
        }

        System.out.printf("Poll last Finish %n");

        while (!pq.isEmpty()) {
            messageNeedToCompare.add(pq.poll().msg);
        }

        System.out.printf("Finish poll rest. %n");
        return ret;
    }

    public static void main(String[] args) throws InterruptedException, MQClientException {

        /*
         * Instantiate with specified consumer group name.
         */
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("please_rename_unique_group_name_4");
        consumer.setConsumeThreadMin(1);
        consumer.setConsumeThreadMax(1);
        consumer.setPullThresholdForQueue(1);
        consumer.setPullThresholdForTopic(1);
        consumer.setClientCallbackExecutorThreads(1);
        consumer.setPersistConsumerOffsetInterval(10);

        /*
         * Specify where to start in case the specified consumer group is a brand new one.
         */
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);

        /*
         * Subscribe one more more topics to consume.
         */
        consumer.subscribe("TopicTest", "*");

        /*
         *  Register callback to execute on arrival of messages fetched from brokers.
         */
        consumer.registerMessageListener(new MessageListenerConcurrently() {

            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs,
                                                            ConsumeConcurrentlyContext context) {
                System.out.printf("Msg received. %n");
                ArrayList<ArrayList<MessageExt>> mps = new ArrayList<ArrayList<MessageExt>>();
                for (MessageExt msg: msgs) {
                    if (msg.getQueueId() >= mps.size())
                        mps.add(new ArrayList<MessageExt>());
                    mps.get(msg.getQueueId()).add(msg);
                }
                System.out.printf("Preparation finished. %n");
                List<MessageExt> res = sort(mps);
                System.out.printf("Sort finished. %n");

                System.out.printf("=======%s Start print msg results: %s %n ========", Thread.currentThread().getName(), res);
                for (MessageExt msg: res){
                    System.out.printf("Receive message with timestamp %d %n", msg.getStoreTimestamp());
                }
                System.out.printf("=======%s Receive New Messages: %s %n=======", Thread.currentThread().getName(), res);
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });

        /*
         *  Launch the consumer instance.
         */
        consumer.start();

        System.out.printf("Consumer Started.%n");
    }
}
