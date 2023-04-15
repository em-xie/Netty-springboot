package com.lld.im.tcp.test;

import org.redisson.Redisson;
import org.redisson.api.RBucket;
import org.redisson.api.RMap;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.api.listener.MessageListener;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.Config;

/**
 * @作者：xie
 * @时间：2023/4/14 16:23
 */
public class RedissonTest {
    public static void main(String[] args) {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://127.0.0.1:6379").setClientName("root").setPassword("123456");
        StringCodec stringCodec = new StringCodec();
        config.setCodec(stringCodec);
        RedissonClient redissonClient = Redisson.create(config);
        RBucket<Object> im = redissonClient.getBucket("im");
        System.out.println(im.get());
        im.set("im");
        System.out.println(im.get());

        RMap<String, String> imMap = redissonClient.getMap("imMap");
        String client = imMap.get("client");
        System.out.println(client);
        imMap.put("client","1111");
        System.out.println(imMap.get("client"));

        RTopic topic = redissonClient.getTopic("topic");
        topic.addListener(String.class, new MessageListener<String>() {
            @Override
            public void onMessage(CharSequence charSequence, String s) {
                System.out.println("1收到消息：" + s);
            }
        });
        topic.publish("nnnnn");


        RTopic topic1 = redissonClient.getTopic("topic");
        topic.addListener(String.class, new MessageListener<String>() {
            @Override
            public void onMessage(CharSequence charSequence, String s) {
                System.out.println("2收到消息：" + s);
            }
        });
        RTopic topic3 = redissonClient.getTopic("topic");
        topic3.publish("11111");
    }
}
