登录消息-保存用户NioSocketChannel

```
package com.lld.im.codec.pack;

import lombok.Data;

/**
 * @description:
 * @author: lld
 * @version: 1.0
 */
@Data
public class LoginPack {

    private String userId;

}

```

```
package com.lld.im.common.enums.command;

/**
 * @description:
 * @author: lld
 * @version: 1.0
 */
public interface Command {
    public int getCommand();
}

```

```
package com.lld.im.common.enums.command;

public enum SystemCommand implements Command {

    //心跳 9999
    PING(0x270f),

    /**
     * 登录 9000
     */
    LOGIN(0x2328),

    //登录ack  9001
    LOGINACK(0x2329),

    //登出  9003
    LOGOUT(0x232b),

    //下线通知 用于多端互斥  9002
    MUTUALLOGIN(0x232a),

    ;

    private int command;

    SystemCommand(int command){
        this.command=command;
    }


    @Override
    public int getCommand() {
        return command;
    }
}

```

```
package com.lld.im.tcp.utils;

import com.lld.im.common.model.UserClientDto;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @作者：xie
 * @时间：2023/4/14 15:46
 */
//存去channel工具类
public  class SessionSocketHolder {
    private static final Map<String, NioSocketChannel> CHANNELS = new ConcurrentHashMap<>();


    public static void put(String userId,NioSocketChannel channel)
    {
        CHANNELS.put(userId,channel);
    }

    public static NioSocketChannel get(String userId)
    {
        return CHANNELS.get(userId);
    }

}


```

```
public class NettyServerHandler extends SimpleChannelInboundHandler<Message> {
    private final static Logger logger = LoggerFactory.getLogger(LimServer.class);
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Message message) throws Exception {
        Integer command = message.getMessageHeader().getCommand();
        if(command== SystemCommand.LOGIN.getCommand())
        {
            LoginPack loginPack = JSONObject.parseObject(JSONObject.toJSONString(message.getMessagePack())
                    , new TypeReference<LoginPack>() {
                    }.getType());
            channelHandlerContext.attr(AttributeKey.valueOf("userId")).set(loginPack.getUserId());
            //将channel存起来
            SessionSocketHolder.put(loginPack.getUserId(), (NioSocketChannel) channelHandlerContext);
        }
    }
}
```

分布式缓存中间件-redisson快速入门操作数据

```
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

```

用户登录网关层-保护用户session

```
package com.lld.im.common.model;

import lombok.Data;

/**
 * @description:
 * @author: lld
 * @version: 1.0
 */
@Data
public class UserSession {

    private String userId;

    /**
     * 应用ID
     */
    private Integer appId;

    /**
     * 端的标识
     */
    private Integer clientType;

    //sdk 版本号
    private Integer version;

    //连接状态 1=在线 2=离线
    private Integer connectState;

    private Integer brokerId;

    private String brokerHost;

    private String imei;

}

```

添加配置文件

```
  redis:
    mode: single # 单机模式：single 哨兵模式：sentinel 集群模式：cluster
    database: 0
    password: 123456
    timeout: 3000 # 超时时间
    poolMinIdle: 8 #最小空闲数
    poolConnTimeout: 3000 # 连接超时时间(毫秒)
    poolSize: 10 # 连接池大小
    single: #redis单机配置
      address: 127.0.0.1:6379
```

```
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RedisConfig {

        /**
         * 单机模式：single 哨兵模式：sentinel 集群模式：cluster
         */
        private String mode;
        /**
         * 数据库
         */
        private Integer database;
        /**
         * 密码
         */
        private String password;
        /**
         * 超时时间
         */
        private Integer timeout;
        /**
         * 最小空闲数
         */
        private Integer poolMinIdle;
        /**
         * 连接超时时间(毫秒)
         */
        private Integer poolConnTimeout;
        /**
         * 连接池大小
         */
        private Integer poolSize;

        /**
         * redis单机配置
         */
        private RedisSingle single;

    }
```

```
 @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RedisSingle {
        /**
         * 地址
         */
        private String address;
    }
```

```
package com.lld.im.tcp.redis;

import com.lld.im.codec.config.BootstrapConfig;
import org.apache.commons.lang3.StringUtils;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;

/**
 * @作者：xie
 * @时间：2023/4/14 18:43
 */
public class SingleClientStrategy {
    public RedissonClient getRedissonClient(BootstrapConfig.RedisConfig redisConfig) {
        Config config = new Config();
        String node = redisConfig.getSingle().getAddress();
        node = node.startsWith("redis://") ? node : "redis://" + node;
        SingleServerConfig serverConfig = config.useSingleServer()
                .setAddress(node)
                .setDatabase(redisConfig.getDatabase())
                .setTimeout(redisConfig.getTimeout())
                .setConnectionMinimumIdleSize(redisConfig.getPoolMinIdle())
                .setConnectTimeout(redisConfig.getPoolConnTimeout())
                .setConnectionPoolSize(redisConfig.getPoolSize());
        if (StringUtils.isNotBlank(redisConfig.getPassword())) {
            serverConfig.setPassword(redisConfig.getPassword());
        }
        StringCodec stringCodec = new StringCodec();
        config.setCodec(stringCodec);
        return Redisson.create(config);
    }
}

```

```
package com.lld.im.tcp.redis;

import com.lld.im.codec.config.BootstrapConfig;
import org.redisson.api.RedissonClient;

/**
 * @作者：xie
 * @时间：2023/4/14 18:41
 */
public class RedisManger {
    private static RedissonClient redissonClient;

    private static Integer loginModel;

    public static void init(BootstrapConfig config){
//        loginModel = config.getLim().getLoginModel();
        SingleClientStrategy singleClientStrategy = new SingleClientStrategy();
        redissonClient = singleClientStrategy.getRedissonClient(config.getLim().getRedis());
//        UserLoginMessageListener userLoginMessageListener = new UserLoginMessageListener(loginModel);
//        userLoginMessageListener.listenerUserLogin();
    }
        //暴露方法 拿到私有变量
        public static RedissonClient getRedissonClient(){
        return redissonClient;
    }
}

```

starter

```
 RedisManger.init(bootstrapConfig);
```

common包常量

```
package com.lld.im.common.constant;

/**
 * @description:
 * @author: lld
 * @version: 1.0
 */
public class Constants {

    /** channel绑定的userId Key*/
    public static final String UserId = "userId";

    /** channel绑定的appId */
    public static final String AppId = "appId";

    public static final String ClientType = "clientType";

    public static final String Imei = "imei";

    /** channel绑定的clientType 和 imel Key*/
    public static final String ClientImei = "clientImei";

    public static final String ReadTime = "readTime";

    public static final String ImCoreZkRoot = "/im-coreRoot";

    public static final String ImCoreZkRootTcp = "/tcp";

    public static final String ImCoreZkRootWeb = "/web";


    public static class RedisConstants{

        /**
         * userSign，格式：appId:userSign:
         */
        public static final String userSign = "userSign";

        /**
         * 用户上线通知channel
         */
        public static final String UserLoginChannel
                = "signal/channel/LOGIN_USER_INNER_QUEUE";


        /**
         * 用户session，appId + UserSessionConstants + 用户id 例如10000：userSession：lld
         */
        public static final String UserSessionConstants = ":userSession:";

        /**
         * 缓存客户端消息防重，格式： appId + :cacheMessage: + messageId
         */
        public static final String cacheMessage = "cacheMessage";

        public static final String OfflineMessage = "offlineMessage";

        /**
         * seq 前缀
         */
        public static final String SeqPrefix = "seq";

        /**
         * 用户订阅列表，格式 ：appId + :subscribe: + userId。Hash结构，filed为订阅自己的人
         */
        public static final String subscribe = "subscribe";

        /**
         * 用户自定义在线状态，格式 ：appId + :userCustomerStatus: + userId。set，value为用户id
         */
        public static final String userCustomerStatus = "userCustomerStatus";

    }

    public static class RabbitConstants{

        public static final String Im2UserService = "pipeline2UserService";

        public static final String Im2MessageService = "pipeline2MessageService";

        public static final String Im2GroupService = "pipeline2GroupService";

        public static final String Im2FriendshipService = "pipeline2FriendshipService";

        public static final String MessageService2Im = "messageService2Pipeline";

        public static final String GroupService2Im = "GroupService2Pipeline";

        public static final String FriendShip2Im = "friendShip2Pipeline";

        public static final String StoreP2PMessage = "storeP2PMessage";

        public static final String StoreGroupMessage = "storeGroupMessage";


    }

    public static class CallbackCommand{
        public static final String ModifyUserAfter = "user.modify.after";

        public static final String CreateGroupAfter = "group.create.after";

        public static final String UpdateGroupAfter = "group.update.after";

        public static final String DestoryGroupAfter = "group.destory.after";

        public static final String TransferGroupAfter = "group.transfer.after";

        public static final String GroupMemberAddBefore = "group.member.add.before";

        public static final String GroupMemberAddAfter = "group.member.add.after";

        public static final String GroupMemberDeleteAfter = "group.member.delete.after";

        public static final String AddFriendBefore = "friend.add.before";

        public static final String AddFriendAfter = "friend.add.after";

        public static final String UpdateFriendBefore = "friend.update.before";

        public static final String UpdateFriendAfter = "friend.update.after";

        public static final String DeleteFriendAfter = "friend.delete.after";

        public static final String AddBlackAfter = "black.add.after";

        public static final String DeleteBlack = "black.delete";

        public static final String SendMessageAfter = "message.send.after";

        public static final String SendMessageBefore = "message.send.before";

    }

    public static class SeqConstants {
        public static final String Message = "messageSeq";

        public static final String GroupMessage = "groupMessageSeq";


        public static final String Friendship = "friendshipSeq";

//        public static final String FriendshipBlack = "friendshipBlackSeq";

        public static final String FriendshipRequest = "friendshipRequestSeq";

        public static final String FriendshipGroup = "friendshipGrouptSeq";

        public static final String Group = "groupSeq";

        public static final String Conversation = "conversationSeq";

    }

}

```

### 用户退出网关层-离线删除用户session

使用常量维护通道key

```
            channelHandlerContext.channel().attr(AttributeKey.valueOf(Constants.UserId)).set(loginPack.getUserId());
            channelHandlerContext.channel().attr(AttributeKey.valueOf(Constants.AppId)).set(message.getMessageHeader().getAppId());
            channelHandlerContext.channel().attr(AttributeKey.valueOf(Constants.ClientType)).set(message.getMessageHeader().getClientType());
```

```
package com.lld.im.common.model;

import lombok.Data;

/**
 * @description:
 * @author: lld
 * @version: 1.0
 */
@Data
public class UserClientDto {

    private Integer appId;

    private Integer clientType;

    private String userId;

    private String imei;

}

```

改造SessionSocketHolder

```
   private static final Map<UserClientDto, NioSocketChannel> CHANNELS = new ConcurrentHashMap<>();


    public static void put(Integer appId,String userId,Integer clientType,NioSocketChannel channel)
    {
        UserClientDto userClientDto = new UserClientDto();
        userClientDto.setAppId(appId);
        userClientDto.setUserId(userId);
        userClientDto.setClientType(clientType);
        CHANNELS.put(userClientDto,channel);
    }

    public static NioSocketChannel get(Integer appId,String userId,Integer clientType)
    {
        UserClientDto userClientDto = new UserClientDto();
        userClientDto.setAppId(appId);
        userClientDto.setUserId(userId);
        userClientDto.setClientType(clientType);
        return CHANNELS.get(userClientDto);
    }

    public static void remove(Integer appId,String userId,Integer clientType)
    {
        UserClientDto userClientDto = new UserClientDto();
        userClientDto.setAppId(appId);
        userClientDto.setUserId(userId);
        userClientDto.setClientType(clientType);
        CHANNELS.remove(userClientDto);
    }

    public static void remove(NioSocketChannel nioSocketChannel)
    {
        CHANNELS.entrySet().stream().filter(entity -> entity.getValue() == nioSocketChannel)
                .forEach(entry -> CHANNELS.remove(entry.getKey()));
    }
```

退出登录

```
else if (command== SystemCommand.LOGOUT.getCommand()) {
            //删除session 与 redis
            String userId  = (String) channelHandlerContext.channel().attr(AttributeKey.valueOf(Constants.UserId)).get();
            Integer appId  = (Integer) channelHandlerContext.channel().attr(AttributeKey.valueOf(Constants.AppId)).get();
            Integer clientType  = (Integer) channelHandlerContext.channel().attr(AttributeKey.valueOf(Constants.ClientType)).get();
            SessionSocketHolder.remove(appId,userId,clientType);
            RedissonClient redissonClient = RedisManger.getRedissonClient();
            RMap<Object, Object> map = redissonClient.getMap(appId + Constants.RedisConstants.UserSessionConstants + userId);
            map.remove(clientType);
            channelHandlerContext.channel().close();
        }
```

### 服务端心跳检测

```
 ch.pipeline().addLast(new IdleStateHandler(
                                0,0,
                                10));
```



添加心跳命令写入时间

```
else if (command== SystemCommand.PING.getCommand()) {
           channelHandlerContext.channel().attr(AttributeKey.valueOf(Constants.ReadTime)).set(System.currentTimeMillis());

        }
```

添加心跳时间 yml config 配置

```
heartBeatTime: 20000 #心跳超时时间 单位毫秒
```

```

        private Long heartBeatTime; //心跳超时时间 单位毫秒
```

获取心跳时间

```
ch.pipeline().addLast(new HeartBeatHandler(config.getHeartBeatTime()));
```

```
package com.lld.im.tcp.handler;

import com.lld.im.common.constant.Constants;
import com.lld.im.tcp.utils.SessionSocketHolder;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

/**
 * @作者：xie
 * @时间：2023/4/15 8:51
 */
@Slf4j
public class HeartBeatHandler extends ChannelInboundHandlerAdapter {
    private Long heartBeatTime;

    public HeartBeatHandler(Long heartBeatTime) {
        this.heartBeatTime = heartBeatTime;
    }
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        // 判断evt是否是IdleStateEvent（用于触发用户事件，包含 读空闲/写空闲/读写空闲 ）
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent)evt;		// 强制类型转换
            if (event.state() == IdleState.READER_IDLE) {
                log.info("读空闲");
            } else if (event.state() == IdleState.WRITER_IDLE) {
                log.info("进入写空闲");
            } else if (event.state() == IdleState.ALL_IDLE) {
                Long lastReadTime = (Long) ctx.channel()
                        .attr(AttributeKey.valueOf(Constants.ReadTime)).get();
                long now = System.currentTimeMillis();

                if(lastReadTime != null && now - lastReadTime > heartBeatTime){
                    //TODO 退后台逻辑
                    SessionSocketHolder.offlineUserSession((NioSocketChannel) ctx.channel());
                }

            }
        }
    }
}

```

判断是否超时进行离线

```
  public static void  removeUserSession(NioSocketChannel nioSocketChannel)
    {
        String userId  = (String) nioSocketChannel.attr(AttributeKey.valueOf(Constants.UserId)).get();
        Integer appId  = (Integer) nioSocketChannel.attr(AttributeKey.valueOf(Constants.AppId)).get();
        Integer clientType  = (Integer) nioSocketChannel.attr(AttributeKey.valueOf(Constants.ClientType)).get();
        SessionSocketHolder.remove(appId,userId,clientType);
        RedissonClient redissonClient = RedisManger.getRedissonClient();
        RMap<Object, Object> map = redissonClient.getMap(appId + Constants.RedisConstants.UserSessionConstants + userId);
        map.remove(clientType);
        nioSocketChannel.close();
    }

    public static void  offlineUserSession(NioSocketChannel nioSocketChannel)
    {
        String userId  = (String) nioSocketChannel.attr(AttributeKey.valueOf(Constants.UserId)).get();
        Integer appId  = (Integer) nioSocketChannel.attr(AttributeKey.valueOf(Constants.AppId)).get();
        Integer clientType  = (Integer) nioSocketChannel.attr(AttributeKey.valueOf(Constants.ClientType)).get();
        SessionSocketHolder.remove(appId,userId,clientType);
        RedissonClient redissonClient = RedisManger.getRedissonClient();
        RMap<String, String> map = redissonClient.getMap(appId + Constants.RedisConstants.UserSessionConstants + userId);
        String session = map.get(clientType.toString());
        if(!StringUtil.isBlank(session))
        {
            UserSession userSession = JSONObject.parseObject(session, UserSession.class);
            userSession.setConnectState(ImConnectStatusEnum.OFFLINE_STATUS.getCode());
            map.put(clientType.toString(),JSONObject.toJSONString(userSession));

        }
        nioSocketChannel.close();
    }

```

### RabbitMQ的安装、发布订阅、路由模式详解

https://www.cnblogs.com/yakniu/p/16183938.html

TCP接入RabbitMQ，打通和逻辑层交互

```
  rabbitmq:
    host: 127.0.0.1
    port: 5672
    virtualHost: /
    userName: guest
    password: guest
```

```
       /**
         * rabbitmq配置
         */
        private Rabbitmq rabbitmq;
        
        
            /**
     * rabbitmq哨兵模式配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Rabbitmq {
        private String host;

        private Integer port;

        private String virtualHost;

        private String userName;

        private String password;
    } 
```

```
package com.lld.im.tcp.utils;

import com.lld.im.codec.config.BootstrapConfig;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

/**
 * @作者：xie
 * @时间：2023/4/15 13:26
 */
public class MqFactory {
    private static ConnectionFactory factory = null;
    private static Channel defaultChannel;
    private static final ConcurrentHashMap<String,Channel> channelMap = new ConcurrentHashMap<>();

    private static Connection getConnection() throws IOException, TimeoutException {
        return factory.newConnection();
    }

    public static Channel getChannel(String channelName) throws IOException, TimeoutException {
        Channel channel = channelMap.get(channelName);
        if(channel == null){
            channel = getConnection().createChannel();
            channelMap.put(channelName,channel);
        }
        return channel;
    }
    public static void init(BootstrapConfig.Rabbitmq rabbitmq)
    {
        if(factory == null)
        {
            factory = new ConnectionFactory();
            factory.setHost(rabbitmq.getHost());
            factory.setPort(rabbitmq.getPort());
            factory.setUsername(rabbitmq.getUserName());
            factory.setPassword(rabbitmq.getPassword());
            factory.setVirtualHost(rabbitmq.getVirtualHost());
        }
    }

}

```

```
MqFactory.init(bootstrapConfig.getLim().getRabbitmq());
```

```
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

```

```
package com.lld.im.tcp.reciver;

import com.alibaba.fastjson.JSONObject;
import com.lld.im.codec.proto.MessagePack;
import com.lld.im.common.constant.Constants;
import com.lld.im.tcp.utils.MqFactory;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * @作者：xie
 * @时间：2023/4/15 13:49
 */
@Slf4j
public class MessageReceive{
    private static void startReceiveMessage() {
        try {
            Channel channel = MqFactory
                    .getChannel(Constants.RabbitConstants.MessageService2Im);
            channel.queueDeclare(Constants.RabbitConstants.MessageService2Im ,
                    true, false, false, null
            );
            channel.queueBind(Constants.RabbitConstants.MessageService2Im ,
                    Constants.RabbitConstants.MessageService2Im,"");

            channel.basicConsume(Constants.RabbitConstants
                            .MessageService2Im, false,
                    new DefaultConsumer(channel) {
                        @Override
                        public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                            //处理服务端消息
                            String s = new String(body);
                            log.info(s);
                        }
                    }
            );
        } catch (Exception e) {

        }
    }

    public static void init() {
        startReceiveMessage();
    }
}

```

### TCP服务注册-Zookeeper注册TCP服务

```
  zkConfig:
    zkAddr: 127.0.0.1:2181
    zkConnectTimeOut: 5000
```

```
        /**
         * zk配置
         */
        private ZkConfig zkConfig;
        
            @Data
    public static class ZkConfig {
        /**
         * zk连接地址
         */
        private String zkAddr;

        /**
         * zk连接超时时间
         */
        private Integer zkConnectTimeOut;
    }
```

生成结点

```
package com.lld.im.tcp.register;

import com.lld.im.common.constant.Constants;
import org.I0Itec.zkclient.ZkClient;

/**
 * @作者：xie
 * @时间：2023/4/15 14:36
 */
public class ZKit {
    private ZkClient zkClient;

    public ZKit(ZkClient zkClient) {
        this.zkClient = zkClient;
    }

    //im-coreRoot/tcp/ip:port
    public void createRootNode(){
        boolean exists = zkClient.exists(Constants.ImCoreZkRoot);
        if(!exists){
            zkClient.createPersistent(Constants.ImCoreZkRoot);
        }
        boolean tcpExists = zkClient.exists(Constants.ImCoreZkRoot +
                Constants.ImCoreZkRootTcp);
        if(!tcpExists){
            zkClient.createPersistent(Constants.ImCoreZkRoot +
                    Constants.ImCoreZkRootTcp);
        }

        boolean webExists = zkClient.exists(Constants.ImCoreZkRoot +
                Constants.ImCoreZkRootWeb);
        if(!webExists){
            zkClient.createPersistent(Constants.ImCoreZkRoot +
                    Constants.ImCoreZkRootWeb);
        }
    }

    //ip+port
    public void createNode(String path){
        if(!zkClient.exists(path)){
            zkClient.createPersistent(path);
        }
    }
}

```

注册结点

```
package com.lld.im.tcp.register;

import com.lld.im.codec.config.BootstrapConfig;
import com.lld.im.common.constant.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @作者：xie
 * @时间：2023/4/15 14:43
 */
public class RegistryZK implements Runnable{
    private static final Logger logger = LoggerFactory.getLogger(RegistryZK.class);

    private final ZKit zKit;

    private final String ip;

    private final BootstrapConfig.TcpConfig tcpConfig;

    public RegistryZK(ZKit zKit, String ip, BootstrapConfig.TcpConfig tcpConfig) {
        this.zKit = zKit;
        this.ip = ip;
        this.tcpConfig = tcpConfig;
    }

    @Override
    public void run() {
        zKit.createRootNode();
        String tcpPath = Constants.ImCoreZkRoot + Constants.ImCoreZkRootTcp + "/" + ip + ":" + tcpConfig.getTcpPort();
        zKit.createNode(tcpPath);
        logger.info("Registry zookeeper tcpPath success, msg=[{}]", tcpPath);

        String webPath =
                Constants.ImCoreZkRoot + Constants.ImCoreZkRootWeb + "/" + ip + ":" + tcpConfig.getWebSocketPort();
        zKit.createNode(webPath);
        logger.info("Registry zookeeper webPath success, msg=[{}]", tcpPath);

    }
}

```

starter配置zk

```
    public static void registerZK(BootstrapConfig config) throws UnknownHostException {
        String hostAddress = InetAddress.getLocalHost().getHostAddress();
        ZkClient zkClient = new ZkClient(config.getLim().getZkConfig().getZkAddr(),
                config.getLim().getZkConfig().getZkConnectTimeOut());
        ZKit zKit = new ZKit(zkClient);
        RegistryZK registryZK = new RegistryZK(zKit,hostAddress,config.getLim());
        Thread thread = new Thread(registryZK);
        thread.start();
    }
```

```
 registerZK(bootstrapConfig);
```

### 服务改造-TCP服务分布式改造

 

```
 brokerId: 1000 // 服务号
```

```
private Integer brokerId;

    public NettyServerHandler(Integer brokerId) {
        this.brokerId = brokerId;
    }

```

```
            userSession.setBrokerId(brokerId);
//            userSession.setImei(msg.getMessageHeader().getImei());
            try{
                InetAddress localHost = InetAddress.getLocalHost();
                userSession.setBrokerHost(localHost.getHostAddress());
            }catch (Exception e){
                e.printStackTrace();
            }
```

```
package com.lld.im.tcp.reciver;

import com.alibaba.fastjson.JSONObject;
import com.lld.im.codec.proto.MessagePack;
import com.lld.im.common.constant.Constants;
import com.lld.im.tcp.utils.MqFactory;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

/**
 * @作者：xie
 * @时间：2023/4/15 13:49
 */
@Slf4j
public class MessageReceive{


    private static String brokerId;
    private static void startReceiveMessage() {
        try {
            Channel channel = MqFactory
                    .getChannel(Constants.RabbitConstants.MessageService2Im + brokerId);
            channel.queueDeclare(Constants.RabbitConstants.MessageService2Im + brokerId,
                    true, false, false, null
            );
            channel.queueBind(Constants.RabbitConstants.MessageService2Im + brokerId,
                    Constants.RabbitConstants.MessageService2Im, brokerId);

            channel.basicConsume(Constants.RabbitConstants
                            .MessageService2Im + brokerId, false,
                    new DefaultConsumer(channel) {
                        @Override
                        public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                            //处理服务端消息
                            String s = new String(body);
                            log.info(s);
                        }
                    }
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void init(String brokerId) {
        if (StringUtils.isBlank(MessageReceive.brokerId)) {
            MessageReceive.brokerId = brokerId;
        }
        startReceiveMessage();
    }
    public static void init() {
        startReceiveMessage();
    }
}

```

```
                        ch.pipeline().addLast(new NettyServerHandler(config.getBrokerId()));
```

### 即时通讯系统支持多端登录模式-应对多端登录的场景（一）



添加imei号 ： 多端设备不同设备号 

通过登录客户端type和设备号进行识别

```
channelHandlerContext.channel().attr(AttributeKey.valueOf(Constants.Imei)).set(message.getMessageHeader().getImei());
```

```
            map.put(message.getMessageHeader().getClientType()+":"+message.getMessageHeader().getImei() ,JSONObject.toJSONString(userSession));

```



### 即时通讯系统支持多端登录模式-应对多端登录的场景（二）

```
package com.lld.im.common;

/**
 * @author: Chackylee
 * @description:
 **/
public enum ClientType {

    WEBAPI(0,"webApi"),
    WEB(1,"web"),
    IOS(2,"ios"),
    ANDROID(3,"android"),
    WINDOWS(4,"windows"),
    MAC(5,"mac"),
            ;

    private int code;
    private String error;

    ClientType(int code, String error){
        this.code = code;
        this.error = error;
    }
    public int getCode() {
        return this.code;
    }

    public String getError() {
        return this.error;
    }



}

```

![image-20230415173211404](E:\java2\netty-springboot\netty笔记\第七章PPT\image-20230415173211404.png)

```
 //发送客户端消息
            UserClientDto userClientDto = new UserClientDto();
            userClientDto.setImei(message.getMessageHeader().getImei());
            userClientDto.setClientType(message.getMessageHeader().getClientType());
            userClientDto.setUserId(loginPack.getUserId());
            userClientDto.setAppId(message.getMessageHeader().getAppId());
            RTopic topic = redissonClient.getTopic(Constants.RedisConstants.UserLoginChannel);
            topic.publish(JSONObject.toJSONString(userClientDto));
```

### 即时通讯系统支持多端登录模式-应对多端登录的场景（三）

```
  loginModel: 3
```

```
            //发送客户端消息
            UserClientDto userClientDto = new UserClientDto();
            userClientDto.setImei(message.getMessageHeader().getImei());
            userClientDto.setClientType(message.getMessageHeader().getClientType());
            userClientDto.setUserId(loginPack.getUserId());
            userClientDto.setAppId(message.getMessageHeader().getAppId());
            RTopic topic = redissonClient.getTopic(Constants.RedisConstants.UserLoginChannel);
            topic.publish(JSONObject.toJSONString(userClientDto));
```

```
package com.lld.im.codec;

import com.alibaba.fastjson.JSONObject;
import com.lld.im.codec.proto.MessagePack;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * @author: Chackylee
 * @description: 消息编码类，私有协议规则，前4位表示长度，接着command4位，后面是数据
 **/
public class MessageEncoder extends MessageToByteEncoder {

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        if(msg instanceof MessagePack){
            MessagePack msgBody = (MessagePack) msg;
            String s = JSONObject.toJSONString(msgBody.getData());
            byte[] bytes = s.getBytes();
            out.writeInt(msgBody.getCommand());
            out.writeInt(bytes.length);
            out.writeBytes(bytes);
        }
    }

}

```

```
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

```

```
package com.lld.im.tcp.reciver;

import com.alibaba.fastjson.JSONObject;
import com.lld.im.codec.proto.MessagePack;
import com.lld.im.common.ClientType;
import com.lld.im.common.constant.Constants;
import com.lld.im.common.enums.DeviceMultiLoginEnum;
import com.lld.im.common.enums.command.SystemCommand;
import com.lld.im.common.model.UserClientDto;
import com.lld.im.tcp.redis.RedisManger;
import com.lld.im.tcp.utils.SessionSocketHolder;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.AttributeKey;
import org.redisson.api.RTopic;
import org.redisson.api.listener.MessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @description:
 * 多端同步：1单端登录：一端在线：踢掉除了本clinetType + imel 的设备
 *          2双端登录：允许pc/mobile 其中一端登录 + web端 踢掉除了本clinetType + imel 以外的web端设备
 *        3 三端登录：允许手机+pc+web，踢掉同端的其他imei 除了web
 *        4 不做任何处理
 * @作者：xie
 * @时间：2023/4/15 17:49
 */
public class UserLoginMessageListener {
    private final static Logger logger = LoggerFactory.getLogger(UserLoginMessageListener.class);

    private Integer loginModel;

    public UserLoginMessageListener(Integer loginModel) {
        this.loginModel = loginModel;
    }

    public void listenerUserLogin() {
        RTopic topic = RedisManger.getRedissonClient().getTopic(Constants.RedisConstants.UserLoginChannel);
        topic.addListener(String.class, new MessageListener<String>() {
            @Override
            public void onMessage(CharSequence charSequence, String msg) {
                logger.info("收到用户上线通知：" + msg);
                UserClientDto dto = JSONObject.parseObject(msg, UserClientDto.class);
                //当前
                List<NioSocketChannel> nioSocketChannels = SessionSocketHolder.get(dto.getAppId(), dto.getUserId());

                for (NioSocketChannel nioSocketChannel : nioSocketChannels) {
                    if (loginModel == DeviceMultiLoginEnum.ONE.getLoginMode()) {
                        Integer clientType = (Integer) nioSocketChannel.attr(AttributeKey.valueOf(Constants.ClientType)).get();
                        String imei = (String) nioSocketChannel.attr(AttributeKey.valueOf(Constants.Imei)).get();

                        if (!(clientType + ":" + imei).equals(dto.getClientType() + ":" + dto.getImei())) {
                            //不相等踢掉登录
                            MessagePack<Object> pack = new MessagePack<>();
                            pack.setToId((String) nioSocketChannel.attr(AttributeKey.valueOf(Constants.UserId)).get());
                            pack.setUserId((String) nioSocketChannel.attr(AttributeKey.valueOf(Constants.UserId)).get());
                            pack.setCommand(SystemCommand.MUTUALLOGIN.getCommand());
                            nioSocketChannel.writeAndFlush(pack);
                        }

                    } else if (loginModel == DeviceMultiLoginEnum.TWO.getLoginMode()) {
                        if (dto.getClientType() == ClientType.WEB.getCode()) {
                            continue;
                        }
                        Integer clientType = (Integer) nioSocketChannel.attr(AttributeKey.valueOf(Constants.ClientType)).get();

                        if (clientType == ClientType.WEB.getCode()) {
                            continue;
                        }
                        String imei = (String) nioSocketChannel.attr(AttributeKey.valueOf(Constants.Imei)).get();
                        if (!(clientType + ":" + imei).equals(dto.getClientType() + ":" + dto.getImei())) {
                            MessagePack<Object> pack = new MessagePack<>();
                            pack.setToId((String) nioSocketChannel.attr(AttributeKey.valueOf(Constants.UserId)).get());
                            pack.setUserId((String) nioSocketChannel.attr(AttributeKey.valueOf(Constants.UserId)).get());
                            pack.setCommand(SystemCommand.MUTUALLOGIN.getCommand());
                            nioSocketChannel.writeAndFlush(pack);
                        }

                    } else if (loginModel == DeviceMultiLoginEnum.THREE.getLoginMode()) {

                        Integer clientType = (Integer) nioSocketChannel.attr(AttributeKey.valueOf(Constants.ClientType)).get();
                        String imei = (String) nioSocketChannel.attr(AttributeKey.valueOf(Constants.Imei)).get();
                        if (dto.getClientType() == ClientType.WEB.getCode()) {
                            continue;
                        }

                        Boolean isSameClient = false;
                        if ((clientType == ClientType.IOS.getCode() ||
                                clientType == ClientType.ANDROID.getCode()) &&
                                (dto.getClientType() == ClientType.IOS.getCode() ||
                                        dto.getClientType() == ClientType.ANDROID.getCode())) {
                            isSameClient = true;
                        }

                        if ((clientType == ClientType.MAC.getCode() ||
                                clientType == ClientType.WINDOWS.getCode()) &&
                                (dto.getClientType() == ClientType.MAC.getCode() ||
                                        dto.getClientType() == ClientType.WINDOWS.getCode())) {
                            isSameClient = true;
                        }

                        if (isSameClient && !(clientType + ":" + imei).equals(dto.getClientType() + ":" + dto.getImei())) {
                            MessagePack<Object> pack = new MessagePack<>();
                            pack.setToId((String) nioSocketChannel.attr(AttributeKey.valueOf(Constants.UserId)).get());
                            pack.setUserId((String) nioSocketChannel.attr(AttributeKey.valueOf(Constants.UserId)).get());
                            pack.setCommand(SystemCommand.MUTUALLOGIN.getCommand());
                            nioSocketChannel.writeAndFlush(pack);
                        }
                    }
                }


            }
        });
    }
}

```

