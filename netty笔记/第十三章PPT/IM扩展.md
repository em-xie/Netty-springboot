### 让陌生人只能发送几条消息-消息回调

```
  //回调
        ResponseVO responseVO = ResponseVO.successResponse();
        if(appConfig.isSendMessageAfterCallback()){
            responseVO = callbackService.beforeCallback(messageContent.getAppId(), Constants.CallbackCommand.SendMessageBefore
                    , JSONObject.toJSONString(messageContent));
        }

        if(!responseVO.isOk()){
            ack(messageContent,responseVO);
            return;
        }

```

```
  if(appConfig.isSendMessageAfterCallback()){
                    callbackService.callback(messageContent.getAppId(),Constants.CallbackCommand.SendMessageAfter,
                            JSONObject.toJSONString(messageContent));
                }
```

改造callback

```
package com.lld.im.service.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @description 共享线程池
 * @author chackylee
 * @param
 * @return
*/
@Service
public class ShareThreadPool {

    private Logger logger = LoggerFactory.getLogger(ShareThreadPool.class);

    private final ThreadPoolExecutor threadPoolExecutor;
    {
        final AtomicInteger tNum = new AtomicInteger(0);

        threadPoolExecutor = new ThreadPoolExecutor(8, 8, 120, TimeUnit.SECONDS, new LinkedBlockingQueue<>(2 << 20), new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setDaemon(true);
                t.setName("SHARE-Processor-" + tNum.getAndIncrement());
                return t;
            }
        });

    }


    private AtomicLong ind = new AtomicLong(0);
    public void submit(Runnable r) {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        ind.incrementAndGet();

        threadPoolExecutor.submit(() -> {
            long start = System.currentTimeMillis();
            try {
                r.run();
            } catch (Exception e) {
                logger.error("ShareThreadPool_ERROR", e);
            } finally {
                long end = System.currentTimeMillis();
                long dur = end - start;
                long andDecrement = ind.decrementAndGet();
                if (dur > 1000) {
                    logger.warn("ShareThreadPool executed taskDone,remanent num = {},slow task fatal warning,costs time = {},stack: {}", andDecrement, dur, stackTrace);
                } else if (dur > 300) {
                    logger.warn("ShareThreadPool executed taskDone,remanent num = {},slow task warning: {},costs time = {},", andDecrement,r, dur);
                } else {
                    logger.debug("ShareThreadPool executed taskDone,remanent num = {}", andDecrement);
                }
            }
        });


    }

}

```

```
    public void callback(Integer appId,String callbackCommand,String jsonBody){
        shareThreadPool.submit(() -> {
            try {
                httpRequestUtils.doPost(appConfig.getCallbackUrl(),Object.class,builderUrlParams(appId,callbackCommand),
                        jsonBody,null);
            }catch (Exception e){
                logger.error("callback 回调{} : {}出现异常 ： {} ",callbackCommand , appId, e.getMessage());
            }
        });
```

### 消息撤回

```
else if (Objects.equals(command, MessageCommand.MSG_RECALL.getCommand())) {
//                撤回消息
                RecallMessageContent messageContent = JSON.parseObject(msg, new TypeReference<RecallMessageContent>() {
                }.getType());
                messageSyncService.recallMessage(messageContent);
            }
```

```
 //修改历史消息的状态
    //修改离线消息的状态
    //ack给发送方
    //发送给同步端
    //分发给消息的接收方
    public void recallMessage(RecallMessageContent content) {

        Long messageTime = content.getMessageTime();
        Long now = System.currentTimeMillis();

        RecallMessageNotifyPack pack = new RecallMessageNotifyPack();
        BeanUtils.copyProperties(content,pack);

        if(120000L < now - messageTime){
            recallAck(pack,ResponseVO.errorResponse(MessageErrorCode.MESSAGE_RECALL_TIME_OUT),content);
            return;
        }

        QueryWrapper<ImMessageBodyEntity> query = new QueryWrapper<>();
        query.eq("app_id",content.getAppId());
        query.eq("message_key",content.getMessageKey());
        ImMessageBodyEntity body = imMessageBodyMapper.selectOne(query);

        if(body == null){
            //TODO ack失败 不存在的消息不能撤回
            recallAck(pack,ResponseVO.errorResponse(MessageErrorCode.MESSAGEBODY_IS_NOT_EXIST),content);
            return;
        }

        if(body.getDelFlag() == DelFlagEnum.DELETE.getCode()){
            recallAck(pack,ResponseVO.errorResponse(MessageErrorCode.MESSAGE_IS_RECALLED),content);

            return;
        }

        body.setDelFlag(DelFlagEnum.DELETE.getCode());
        imMessageBodyMapper.update(body,query);

        if(content.getConversationType() == ConversationTypeEnum.P2P.getCode()){

            // 找到fromId的队列
            String fromKey = content.getAppId() + ":" + Constants.RedisConstants.OfflineMessage + ":" + content.getFromId();
            // 找到toId的队列
            String toKey = content.getAppId() + ":" + Constants.RedisConstants.OfflineMessage + ":" + content.getToId();

            OfflineMessageContent offlineMessageContent = new OfflineMessageContent();
            BeanUtils.copyProperties(content,offlineMessageContent);
            offlineMessageContent.setDelFlag(DelFlagEnum.DELETE.getCode());
            offlineMessageContent.setMessageKey(content.getMessageKey());
            offlineMessageContent.setConversationType(ConversationTypeEnum.P2P.getCode());
            offlineMessageContent.setConversationId(conversationService.convertConversationId(offlineMessageContent.getConversationType()
                    ,content.getFromId(),content.getToId()));
            offlineMessageContent.setMessageBody(body.getMessageBody());

            long seq = redisSeq.doGetSeq(content.getAppId() + ":" + Constants.SeqConstants.Message + ":" + ConversationIdGenerate.generateP2PId(content.getFromId(),content.getToId()));
            offlineMessageContent.setMessageSequence(seq);

            long messageKey = SnowflakeIdWorker.nextId();

            redisTemplate.opsForZSet().add(fromKey,JSONObject.toJSONString(offlineMessageContent),messageKey);
            redisTemplate.opsForZSet().add(toKey,JSONObject.toJSONString(offlineMessageContent),messageKey);

            //ack
            recallAck(pack,ResponseVO.successResponse(),content);
            //分发给同步端
            messageProducer.sendToUserExceptClient(content.getFromId(),
                    MessageCommand.MSG_RECALL_NOTIFY,pack,content);
            //分发给对方
            messageProducer.sendToUser(content.getToId(),MessageCommand.MSG_RECALL_NOTIFY,
                    pack,content.getAppId());
        }else{
            List<String> groupMemberId = imGroupMemberService.getGroupMemberId(content.getToId(), content.getAppId());
            long seq = redisSeq.doGetSeq(content.getAppId() + ":" + Constants.SeqConstants.Message + ":" + ConversationIdGenerate.generateP2PId(content.getFromId(),content.getToId()));
            //ack
            recallAck(pack,ResponseVO.successResponse(),content);
            //发送给同步端
            messageProducer.sendToUserExceptClient(content.getFromId(), MessageCommand.MSG_RECALL_NOTIFY, pack
                    , content);
            for (String memberId : groupMemberId) {
                String toKey = content.getAppId() + ":" + Constants.SeqConstants.Message + ":" + memberId;
                OfflineMessageContent offlineMessageContent = new OfflineMessageContent();
                offlineMessageContent.setDelFlag(DelFlagEnum.DELETE.getCode());
                BeanUtils.copyProperties(content,offlineMessageContent);
                offlineMessageContent.setConversationType(ConversationTypeEnum.GROUP.getCode());
                offlineMessageContent.setConversationId(conversationService.convertConversationId(offlineMessageContent.getConversationType()
                        ,content.getFromId(),content.getToId()));
                offlineMessageContent.setMessageBody(body.getMessageBody());
                offlineMessageContent.setMessageSequence(seq);
                redisTemplate.opsForZSet().add(toKey,JSONObject.toJSONString(offlineMessageContent),seq);

                groupMessageProducer.producer(content.getFromId(), MessageCommand.MSG_RECALL_NOTIFY, pack,content);
            }
        }

    }

    private void recallAck(RecallMessageNotifyPack recallPack, ResponseVO<Object> success, ClientInfo clientInfo) {
        ResponseVO<Object> wrappedResp = success;
        messageProducer.sendToUser(recallPack.getFromId(),
                MessageCommand.MSG_RECALL_ACK, wrappedResp, clientInfo);
    }
```

### 设计亿级聊天记录存储方案

![image-20230420145435430](E:\java2\netty-springboot\netty笔记\第十三章PPT\image-20230420145435430.png)

根据appid进行分库-如果业务线不多的情况下可以这样进行。



一个库也由可以存不下

一、做好数据迁移备份，可以拉起7天的消息等。



二、分表

```
package com.lld.im.service.utils;



import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author: Chackylee
 * @description: 消息key生成
 **/
public class MessageKeyGenerate {

    //标识从2020.1.1开始
    private static final long T202001010000 = 1577808000000L;

//    private Lock lock = new ReentrantLock();
    AtomicReference<Thread> owner = new AtomicReference<>();

    private static volatile int rotateId = 0;
    private static int rotateIdWidth = 15;

    private static int rotateIdMask = 32767;
    private static volatile long timeId = 0;

    private int nodeId = 0;
    private static int nodeIdWidth = 6;
    private static int nodeIdMask = 63;


    public void setNodeId(int nodeId) {
        this.nodeId = nodeId;
    }

    /**
     * ID = timestamp(43) + nodeId(6) + rotateId(15)
     *
     */
    public synchronized long generateId() throws Exception {

//        lock.lock();

        this.lock();

        rotateId = rotateId + 1;

        long id = System.currentTimeMillis() - T202001010000;

        //不同毫秒数生成的id要重置timeId和自选次数
        if (id > timeId) {
            timeId = id;
            rotateId = 1;
        } else if (id == timeId) {
            //表示是同一毫秒的请求
            if (rotateId == rotateIdMask) {
                //一毫秒只能发送32768到这里表示当前毫秒数已经超过了
                while (id <= timeId) {
                    //重新给id赋值
                    id = System.currentTimeMillis() - T202001010000;
                }
                this.unLock();
                return generateId();
            }
        }

        id <<= nodeIdWidth;
        id += (nodeId & nodeIdMask);


        id <<= rotateIdWidth;
        id += rotateId;

//        lock.unlock();
        this.unLock();
        return id;
    }

    public static int getSharding(long mid) {

        Calendar calendar = Calendar.getInstance();

        mid >>= nodeIdWidth;
        mid >>= rotateIdWidth;

        calendar.setTime(new Date(T202001010000 + mid));

        int month = calendar.get(Calendar.MONTH);
        int year = calendar.get(Calendar.YEAR);
        year %= 3;

        return (year * 12 + month);
    }

    public static long getMsgIdFromTimestamp(long timestamp) {
        long id = timestamp - T202001010000;

        id <<= rotateIdWidth;
        id <<= nodeIdWidth;

        return id;
    }

    public void lock() {
        Thread cur = Thread.currentThread();
        //lock函数将owner设置为当前线程，并且预测原来的值为空。
        // unlock函数将owner设置为null，并且预测值为当前线程。
        // 当有第二个线程调用lock操作时由于owner值不为空，导致循环
        //一直被执行，直至第一个线程调用unlock函数将owner设置为null，第二个线程才能进入临界区。
        while (!owner.compareAndSet(null, cur)){
        }
    }
    public void unLock() {
        Thread cur = Thread.currentThread();
        owner.compareAndSet(cur, null);
    }

    public static void main(String[] args) throws Exception {
//        try {
//            Calendar calendar = Calendar.getInstance();
//            MessageKeyGenerate messageKeyGenerate = new MessageKeyGenerate();
//            long msgIdFromTimestamp = getMsgIdFromTimestamp(1678459712000L);
//            System.out.println(getSharding(msgIdFromTimestamp));
//        } catch (Exception e) {
//
//        }
        MessageKeyGenerate messageKeyGenerate = new MessageKeyGenerate();
        for (int i = 0; i < 10; i++) {
            long l = messageKeyGenerate.generateId();
            System.out.println(l);
        }
//        MessageKeyGenerate messageKeyGenerate = new MessageKeyGenerate();
//        long l = messageKeyGenerate.generateId();
//        System.out.println("生成了一个id：" + l);
//        int sharding = getSharding(l);
//        System.out.println("解密id的时间戳：" + sharding);

        //im_message_history_12


        //10000  10001
        //0      1

        long msgIdFromTimestamp = getMsgIdFromTimestamp(1734529845000L);
        int sharding = getSharding(msgIdFromTimestamp);
        System.out.println(sharding);
    }

}

```

### 视频通话实现思路和流程

![image-20230420161528592](E:\java2\netty-springboot\netty笔记\第十三章PPT\image-20230420161528592.png)

![image-20230420163917800](E:\java2\netty-springboot\netty笔记\第十三章PPT\image-20230420163917800.png)