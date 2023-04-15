package com.lld.im.tcp.publish;

import com.alibaba.fastjson.JSONObject;
import com.lld.im.tcp.utils.MqFactory;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;

/**
 * @作者：xie
 * @时间：2023/4/15 13:44
 */

@Slf4j
public class MqMessageProducer {
    public static void sendMessage(Object message)
    {
        Channel channel = null;
        String channelName = "";

        try {
            channel = MqFactory.getChannel(channelName);
            channel.basicPublish(channelName,"",null, JSONObject.toJSONString(message).getBytes());
        }catch (Exception e)
        {
            log.error("发送消息异常：{}",e.getMessage());
        }
    }
}
