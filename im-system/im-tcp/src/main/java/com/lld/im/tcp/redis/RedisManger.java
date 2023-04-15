package com.lld.im.tcp.redis;

import com.lld.im.codec.config.BootstrapConfig;
import com.lld.im.tcp.reciver.UserLoginMessageListener;
import org.redisson.api.RedissonClient;

/**
 * @作者：xie
 * @时间：2023/4/14 18:41
 */
public class RedisManger {
    private static RedissonClient redissonClient;

    private static Integer loginModel;

    public static void init(BootstrapConfig config){
        loginModel = config.getLim().getLoginModel();
        SingleClientStrategy singleClientStrategy = new SingleClientStrategy();
        redissonClient = singleClientStrategy.getRedissonClient(config.getLim().getRedis());
        UserLoginMessageListener userLoginMessageListener = new UserLoginMessageListener(loginModel);
        userLoginMessageListener.listenerUserLogin();
    }
        //暴露方法 拿到私有变量
        public static RedissonClient getRedissonClient(){
        return redissonClient;
    }
}
