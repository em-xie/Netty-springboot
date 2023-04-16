### 负载均衡策略-随机模式

面向接口编程

```
package com.lld.im.common.router;

import java.util.List;

/**
 * @作者：xie
 * @时间：2023/4/15 20:43
 */
public interface RouterHandle {
    public String routerService(List<String> values,String key);
}

```

随机

```
package com.lld.im.common.router.algorithm.random;

import com.lld.im.common.enums.UserErrorCode;
import com.lld.im.common.exception.ApplicationException;
import com.lld.im.common.router.RouterHandle;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @作者：xie
 * @时间：2023/4/15 20:45
 */
public class RandomHandle implements RouterHandle {
    @Override
    public String routerService(List<String> values, String key) {
        int size = values.size();
        if(size == 0)
        {
            throw new ApplicationException(UserErrorCode.SERVER_NOT_AVAILABLE);
        }
        int i = ThreadLocalRandom.current().nextInt(size);
        return values.get(i);
    }
}

```

注册到bean中

```
package com.lld.im.service.config;

import com.lld.im.common.router.RouterHandle;
import com.lld.im.common.router.algorithm.random.RandomHandle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @description:
 * @author: lld
 * @version: 1.0
 */
@Configuration
public class BeanConfig {
    @Bean
    public RouterHandle routerHandle()
    {
        return new RandomHandle();
    }
}

```

配置zk

```
appConfig:
  zkAddr: 127.0.0.1:2181 # zk连接地址
  zkConnectTimeOut: 50000 #zk超时时间

```

```
package com.lld.im.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author: Chackylee
 * @description:
 **/
@Data
@Component
@ConfigurationProperties(prefix = "appconfig")
public class AppConfig {

//    private String privateKey;

    /** zk连接地址*/
    private String zkAddr;

    /** zk连接超时时间*/
    private Integer zkConnectTimeOut;

//    /** im管道地址路由策略*/
//    private Integer imRouteWay;
//
//    private boolean sendMessageCheckFriend; //发送消息是否校验关系链
//
//    private boolean sendMessageCheckBlack; //发送消息是否校验黑名单
//
//    /** 如果选用一致性hash的话具体hash算法*/
//    private Integer consistentHashWay;
//
//    private String callbackUrl;
//
//    private boolean modifyUserAfterCallback; //用户资料变更之后回调开关
//
//    private boolean addFriendAfterCallback; //添加好友之后回调开关
//
//    private boolean addFriendBeforeCallback; //添加好友之前回调开关
//
//    private boolean modifyFriendAfterCallback; //修改好友之后回调开关
//
//    private boolean deleteFriendAfterCallback; //删除好友之后回调开关
//
//    private boolean addFriendShipBlackAfterCallback; //添加黑名单之后回调开关
//
//    private boolean deleteFriendShipBlackAfterCallback; //删除黑名单之后回调开关
//
//    private boolean createGroupAfterCallback; //创建群聊之后回调开关
//
//    private boolean modifyGroupAfterCallback; //修改群聊之后回调开关
//
//    private boolean destroyGroupAfterCallback;//解散群聊之后回调开关
//
//    private boolean deleteGroupMemberAfterCallback;//删除群成员之后回调
//
//    private boolean addGroupMemberBeforeCallback;//拉人入群之前回调
//
//    private boolean addGroupMemberAfterCallback;//拉人入群之后回调
//
//    private boolean sendMessageAfterCallback;//发送单聊消息之后
//
//    private boolean sendMessageBeforeCallback;//发送单聊消息之前
//
//    private Integer deleteConversationSyncMode;
//
//    private Integer offlineMessageCount;//离线消息最大条数

}

```

注册bean

```
    @Autowired
    AppConfig appConfig;

    @Bean
    public ZkClient buildZkClient()
    {
        return new ZkClient(appConfig.getZkAddr(),appConfig.getZkConnectTimeOut());
    }
```

扫包

```
@SpringBootApplication(scanBasePackages = {"com.lld.im.common",
        "com.lld.im.service"})
```

user controller

```
@RequestMapping("/login")
    public ResponseVO login(@RequestBody @Validated LoginReq req, Integer appId) {
        ResponseVO login = imUserService.login();
        if(login.isOk())
        {
            //去zk获取一个im的地址，返回给sdk
            List<String> allNode = new ArrayList<>();
            if(req.getClientType() == ClientType.WEB.getCode())
            {
                allNode = zKit.getAllWebNode();
            }else {
                allNode = zKit.getAllTcpNode();
            }
            String s = routerHandle.routerService(allNode, req.getUserId());
            RouteInfo parse = RouteInfoParseUtil.parse(s);
            return ResponseVO.successResponse(parse);

        }
        return ResponseVO.errorResponse();
    }

```

分开zk获取的地址端口

```
package com.lld.im.common.utils;


import com.lld.im.common.BaseErrorCode;
import com.lld.im.common.exception.ApplicationException;
import com.lld.im.common.router.RouteInfo;

/**
 *
 * @since JDK 1.8
 */
public class RouteInfoParseUtil {

    public static RouteInfo parse(String info){
        try {
            String[] serverInfo = info.split(":");
            RouteInfo routeInfo =  new RouteInfo(serverInfo[0], Integer.parseInt(serverInfo[1])) ;
            return routeInfo ;
        }catch (Exception e){
            throw new ApplicationException(BaseErrorCode.PARAMETER_ERROR) ;
        }
    }
}

```

### 负载均衡策略-轮询模式

[(72条消息) 高并发编程之AtomicLong讲解_住手丶让我来的博客-CSDN博客](https://blog.csdn.net/weixin_42146366/article/details/87820373)

```
package com.lld.im.common.router.algorithm.loop;

import com.lld.im.common.enums.UserErrorCode;
import com.lld.im.common.exception.ApplicationException;
import com.lld.im.common.router.RouterHandle;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @作者：xie
 * @时间：2023/4/16 8:21
 */
public class LoopHandle implements RouterHandle {
    private final AtomicLong index = new AtomicLong();
    @Override
    public String routerService(List<String> values, String key) {

        int size = values.size();
        if(size == 0)
        {
            throw new ApplicationException(UserErrorCode.SERVER_NOT_AVAILABLE);
        }
        long l = index.incrementAndGet() % size;
        if(l < 0)
        {
            l = 0L;
        }
        return values.get((int) l);
    }
}

```

```
  @Bean
    public RouterHandle routerHandle()
    {
        return new LoopHandle();
    }
```

### 负载均衡策略-一致性hash方式

```
package com.lld.im.common.router.algorithm.consistenthash;

import com.lld.im.common.router.RouterHandle;

import java.util.List;

/**
 * @description:
 * @author: lld
 * @version: 1.0
 */
public class ConsistentHashHandle implements RouterHandle {

    //TreeMap
    private AbstractConsistentHash hash;

    public void setHash(AbstractConsistentHash hash) {
        this.hash = hash;
    }



    @Override
    public String routerService(List<String> values, String key) {
        return hash.process(values,key);
    }
}

```

```
package com.lld.im.common.router.algorithm.consistenthash;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * @description: 一致性hash 抽象类
 * @author: lld
 * @version: 1.0
 */
public abstract class AbstractConsistentHash {

    //add
    protected abstract void add(long key,String value);

    //sort
    protected void sort(){}

    //获取节点 get
    protected abstract String getFirstNodeValue(String value);

    /**
     * 处理之前事件
     */
    protected abstract void processBefore();

    /**
     * 传入节点列表以及客户端信息获取一个服务节点
     * @param values
     * @param key
     * @return
     */
    //synchronized保证线程安全
    public synchronized String process(List<String> values, String key){
        processBefore();
        for (String value : values) {
            add(hash(value), value);
        }
        sort();
        return getFirstNodeValue(key) ;
    }


    //hash
    /**
     * hash 运算
     * @param value
     * @return
     */
    public Long hash(String value){
        MessageDigest md5;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 not supported", e);
        }
        md5.reset();
        byte[] keyBytes = null;
        try {
            keyBytes = value.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Unknown string :" + value, e);
        }

        md5.update(keyBytes);
        byte[] digest = md5.digest();

        // hash code, Truncate to 32-bits
        long hashCode = ((long) (digest[3] & 0xFF) << 24)
                | ((long) (digest[2] & 0xFF) << 16)
                | ((long) (digest[1] & 0xFF) << 8)
                | (digest[0] & 0xFF);

        long truncateHashCode = hashCode & 0xffffffffL;
        return truncateHashCode;
    }

}

```

```
package com.lld.im.common.router.algorithm.consistenthash;

import com.lld.im.common.enums.UserErrorCode;
import com.lld.im.common.exception.ApplicationException;

import java.util.SortedMap;
import java.util.TreeMap;

/**
 * @description:
 * @author: lld
 * @version: 1.0
 */
public class TreeMapConsistentHash extends AbstractConsistentHash {

    private TreeMap<Long,String> treeMap = new TreeMap<>();

    private static final int NODE_SIZE = 2;

    @Override
    protected void add(long key, String value) {
        //虚拟节点
        //一个结点有3个值
        for (int i = 0; i < NODE_SIZE; i++) {
            treeMap.put(super.hash("node" + key +i),value);
        }
        treeMap.put(key,value);
    }

    
    @Override
    protected String getFirstNodeValue(String value) {
        //用户id
        Long hash = super.hash(value);
        SortedMap<Long, String> last = treeMap.tailMap(hash);
        if(!last.isEmpty()){
            return last.get(last.firstKey());
        }

        if (treeMap.size() == 0){
            throw new ApplicationException(UserErrorCode.SERVER_NOT_AVAILABLE) ;
        }

        return treeMap.firstEntry().getValue();
    }

    @Override
    protected void processBefore() {
        treeMap.clear();
    }
}

```

### 配置文件配置负载均衡策略

反射实现

[(72条消息) Java--invoke方法（反射）_寻鹿_的博客-CSDN博客](https://blog.csdn.net/L1258244077/article/details/127867377)

```
 @Bean
    public RouterHandle routerHandle() throws Exception{
        Integer imRouteWay = appConfig.getImRouteWay();
        String routeWay = "";
        ImUrlRouteWayEnum handler = ImUrlRouteWayEnum.getHandler(imRouteWay);
        assert handler != null;
        routeWay = handler.getClazz();
        RouterHandle routerHandle = (RouterHandle) Class.forName(routeWay).newInstance();
        if(handler == ImUrlRouteWayEnum.HASH)
        {
            Method setHash = Class.forName(routeWay).getMethod("setHash", AbstractConsistentHash.class);
            Integer consistentHashWay = appConfig.getConsistentHashWay();
            String hashWay = "";
            RouteHashMethodEnum hashMethodHandler = RouteHashMethodEnum.getHandler(consistentHashWay);
            assert hashMethodHandler != null;
            hashWay = hashMethodHandler.getClazz();
            AbstractConsistentHash abstractConsistentHash = (AbstractConsistentHash) Class.forName(hashWay).newInstance();
            setHash.invoke(routerHandle,abstractConsistentHash);
        }
        return routerHandle;
    }
```

yml添加

```
  imRouteWay: 3 # 路由策略1轮训 2随机 3hash
  consistentHashWay: 1 # 如果选用一致性hash的话具体hash算法 1 TreeMap 2 自定义Map
```

```
package com.lld.im.common.enums;

public enum ImUrlRouteWayEnum {

    /**
     * 随机
     */
    RAMDOM(1,"com.lld.im.common.router.algorithm.random.RandomHandle"),


    /**
     * 1.轮训
     */
    LOOP(2,"com.lld.im.common.router.algorithm.loop.LoopHandle"),

    /**
     * HASH
     */
    HASH(3,"com.lld.im.common.router.algorithm.consistenthash.ConsistentHashHandle"),
    ;


    private int code;
    private String clazz;

    /**
     * 不能用 默认的 enumType b= enumType.values()[i]; 因为本枚举是类形式封装
     * @param ordinal
     * @return
     */
    public static ImUrlRouteWayEnum getHandler(int ordinal) {
        for (int i = 0; i < ImUrlRouteWayEnum.values().length; i++) {
            if (ImUrlRouteWayEnum.values()[i].getCode() == ordinal) {
                return ImUrlRouteWayEnum.values()[i];
            }
        }
        return null;
    }

    ImUrlRouteWayEnum(int code, String clazz){
        this.code=code;
        this.clazz=clazz;
    }

    public String getClazz() {
        return clazz;
    }

    public int getCode() {
        return code;
    }
}

```

```
package com.lld.im.common.enums;

public enum RouteHashMethodEnum {

    /**
     * TreeMap
     */
    TREE(1,"com.lld.im.common.router.algorithm.consistenthash" +
            ".TreeMapConsistentHash"),

    /**
     * 自定义map
     */
    CUSTOMER(2,"com.lld.im.common.router.algorithm.consistenthash.xxxx"),

    ;


    private int code;
    private String clazz;

    /**
     * 不能用 默认的 enumType b= enumType.values()[i]; 因为本枚举是类形式封装
     * @param ordinal
     * @return
     */
    public static RouteHashMethodEnum getHandler(int ordinal) {
        for (int i = 0; i < RouteHashMethodEnum.values().length; i++) {
            if (RouteHashMethodEnum.values()[i].getCode() == ordinal) {
                return RouteHashMethodEnum.values()[i];
            }
        }
        return null;
    }

    RouteHashMethodEnum(int code, String clazz){
        this.code=code;
        this.clazz=clazz;
    }

    public String getClazz() {
        return clazz;
    }

    public int getCode() {
        return code;
    }
}

```

### 使用Apache-HttpClient封装http请求工具

```
  callbackUrl: http://127.0.0.1:8000/callback
```

```
  private String callbackUrl;
```

```
package com.lld.im.service.utils;

import com.lld.im.common.ResponseVO;
import com.lld.im.common.config.AppConfig;
import com.lld.im.common.utils.HttpRequestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * @作者：xie
 * @时间：2023/4/16 10:19
 */
@Component
public class CallbackService {
    private Logger logger = LoggerFactory.getLogger(CallbackService.class);

    @Autowired
    HttpRequestUtils httpRequestUtils;

    @Autowired
    AppConfig appConfig;

//    @Autowired
//    ShareThreadPool shareThreadPool;


    public void callback(Integer appId,String callbackCommand,String jsonBody){
//        shareThreadPool.submit(() -> {
//            try {
//                httpRequestUtils.doPost(appConfig.getCallbackUrl(),Object.class,builderUrlParams(appId,callbackCommand),
//                        jsonBody,null);
//            }catch (Exception e){
//                logger.error("callback 回调{} : {}出现异常 ： {} ",callbackCommand , appId, e.getMessage());
//            }
//        });
            try {
                httpRequestUtils.doPost(appConfig.getCallbackUrl(),Object.class,builderUrlParams(appId,callbackCommand),
                        jsonBody,null);
            }catch (Exception e){
                logger.error("callback 回调{} : {}出现异常 ： {} ",callbackCommand , appId, e.getMessage());

            }

    }

    public ResponseVO beforeCallback(Integer appId, String callbackCommand, String jsonBody){
        try {
            ResponseVO responseVO = httpRequestUtils.doPost("", ResponseVO.class, builderUrlParams(appId, callbackCommand),
                    jsonBody, null);
            return responseVO;
        }catch (Exception e){
            logger.error("callback 之前 回调{} : {}出现异常 ： {} ",callbackCommand , appId, e.getMessage());
            return ResponseVO.successResponse();
        }
    }

    public Map builderUrlParams(Integer appId, String command) {
        Map map = new HashMap();
        map.put("appId", appId);
        map.put("command", command);
        return map;
    }
}

```

### 编写用户资料变更回调

配置是否开启回调yml

```
  modifyUserAfterCallback: false # 用户资料变更之后回调开关
```



```
   //更新成功回调
            if(config.isModifyUserAfterCallback())
            {
                callbackService.callback(req.getAppId(),
                        Constants.CallbackCommand.ModifyUserAfter,JSONObject.toJSONString(req));
            }
```



### 好友模块回调

更新删除的操作进行模块回调

```
 //之前回调
        if(appConfig.isAddFriendBeforeCallback())
        {
            ResponseVO responseVO = callbackService.beforeCallback(req.getAppId(),
                    Constants.CallbackCommand.AddFriendBefore,
                    JSONObject.toJSONString(req));
            if(!responseVO.isOk())
            {
                return responseVO;
            }
        }

```



```
 //之后回调
            if (appConfig.isAddFriendShipBlackAfterCallback()){
                AddFriendBlackAfterCallbackDto callbackDto = new AddFriendBlackAfterCallbackDto();
                callbackDto.setFromId(req.getFromId());
                callbackDto.setToId(req.getToId());
                callbackService.beforeCallback(req.getAppId(),
                        Constants.CallbackCommand.DeleteBlack, JSONObject
                                .toJSONString(callbackDto));
            }
```

群组模块回调

```
    if(responseVO.isOk()) {
            if (appConfig.isDeleteGroupMemberAfterCallback()) {
                callbackService.callback(req.getAppId(),
                        Constants.CallbackCommand.GroupMemberDeleteAfter,
                        JSONObject.toJSONString(req));
            }
        }
```

```
if(appConfig.isAddGroupMemberBeforeCallback()){

            ResponseVO responseVO = callbackService.beforeCallback(req.getAppId(), Constants.CallbackCommand.GroupMemberAddBefore
                    , JSONObject.toJSONString(req));
            if(!responseVO.isOk()){
                return responseVO;
            }

            try {
                memberDtos
                        = JSONArray.parseArray(
                                JSONObject.toJSONString(responseVO.getData()),
                        GroupMemberDto.class);
            }catch (Exception e){
                e.printStackTrace();
                log.error("GroupMemberAddBefore 回调失败：{}",req.getAppId());
            }
        }
```

### 启动一个新的应用服务验证回调机制

im-app-service回调地址进行测试

![image-20230416144532324](E:\java2\netty-springboot\netty笔记\第八章PPT\image-20230416144532324.png)

![image-20230416144543176](E:\java2\netty-springboot\netty笔记\第八章PPT\image-20230416144543176.png)

### 封装查询用户session工具类

```
package com.lld.im.service.utils;

import com.alibaba.fastjson.JSONObject;
import com.lld.im.common.constant.Constants;
import com.lld.im.common.enums.ImConnectStatusEnum;
import com.lld.im.common.model.UserSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @作者：xie
 * @时间：2023/4/16 15:06
 */
@Component
public class UserSessionUtils {
    @Autowired
    StringRedisTemplate stringRedisTemplate;

    //1.获取用户所有的session

    public List<UserSession> getUserSession(Integer appId, String userId){

        String userSessionKey = appId + Constants.RedisConstants.UserSessionConstants
                + userId;
        Map<Object, Object> entries =
                stringRedisTemplate.opsForHash().entries(userSessionKey);
        List<UserSession> list = new ArrayList<>();
        Collection<Object> values = entries.values();
        for (Object o : values){
            String str = (String) o;
            UserSession session =
                    JSONObject.parseObject(str, UserSession.class);
            if(Objects.equals(session.getConnectState(), ImConnectStatusEnum.ONLINE_STATUS.getCode())){
                list.add(session);
            }
        }
        return list;
    }

    //2.获取用户除了本端的session

    //1.获取用户所有的session

    public UserSession getUserSession(Integer appId,String userId
            ,Integer clientType,String imei){

        String userSessionKey = appId + Constants.RedisConstants.UserSessionConstants
                + userId;
        String hashKey = clientType + ":" + imei;
        Object o = stringRedisTemplate.opsForHash().get(userSessionKey, hashKey);
        return JSONObject.parseObject(Objects.requireNonNull(o).toString(), UserSession.class);
    }
}

```

封装messageProducer给用户发送消息

```
package com.lld.im.service.utils;

import com.alibaba.fastjson.JSONObject;
import com.lld.im.codec.proto.MessagePack;
import com.lld.im.common.enums.command.Command;
import com.lld.im.common.model.ClientInfo;
import com.lld.im.common.model.UserSession;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @作者：xie
 * @时间：2023/4/16 15:21
 */
@Component
public class MessageProducer {

    private static Logger logger = LoggerFactory.getLogger(MessageProducer.class);

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    UserSessionUtils userSessionUtils;

    public boolean sendMessage(UserSession session, Object msg){
        try {
            logger.info("send message == " + msg);
            rabbitTemplate.convertAndSend("",session.getBrokerId()+"",msg);
            return true;
        }catch (Exception e){
            logger.error("send error :" + e.getMessage());
            return false;
        }
    }

    //包装数据，调用sendMessage
    public boolean sendPack(String toId, Command command, Object msg, UserSession session){
        MessagePack messagePack = new MessagePack();
        messagePack.setCommand(command.getCommand());
        messagePack.setToId(toId);
        messagePack.setClientType(session.getClientType());
        messagePack.setAppId(session.getAppId());
        messagePack.setImei(session.getImei());
        JSONObject jsonObject = JSONObject.parseObject(JSONObject.toJSONString(msg));
        messagePack.setData(jsonObject);

        String body = JSONObject.toJSONString(messagePack);
        return sendMessage(session, body);
    }

    //发送给所有端的方法
    public List<ClientInfo> sendToUser(String toId,Command command,Object data,Integer appId){
        List<UserSession> userSession
                = userSessionUtils.getUserSession(appId, toId);
        List<ClientInfo> list = new ArrayList<>();
        for (UserSession session : userSession) {
            boolean b = sendPack(toId, command, data, session);
            if(b){
                list.add(new ClientInfo(session.getAppId(),session.getClientType(),session.getImei()));
            }
        }
        return list;
    }

    public void sendToUser(String toId, Integer clientType,String imei, Command command,
                           Object data, Integer appId){
        if(clientType != null && StringUtils.isNotBlank(imei)){
            ClientInfo clientInfo = new ClientInfo(appId, clientType, imei);
            sendToUserExceptClient(toId,command,data,clientInfo);
        }else{
            sendToUser(toId,command,data,appId);
        }
    }

    //发送给某个用户的指定客户端
    public void sendToUser(String toId, Command command
            , Object data, ClientInfo clientInfo){
        UserSession userSession = userSessionUtils.getUserSession(clientInfo.getAppId(), toId, clientInfo.getClientType(),
                clientInfo.getImei());
        sendPack(toId,command,data,userSession);
    }

    private boolean isMatch(UserSession sessionDto, ClientInfo clientInfo) {
        return Objects.equals(sessionDto.getAppId(), clientInfo.getAppId())
                && Objects.equals(sessionDto.getImei(), clientInfo.getImei())
                && Objects.equals(sessionDto.getClientType(), clientInfo.getClientType());
    }

    //发送给除了某一端的其他端
    public void sendToUserExceptClient(String toId, Command command
            , Object data, ClientInfo clientInfo){
        List<UserSession> userSession = userSessionUtils
                .getUserSession(clientInfo.getAppId(),
                        toId);
        for (UserSession session : userSession) {
            if(!isMatch(session,clientInfo)){
                sendPack(toId,command,data,session);
            }
        }
    }

}

```

### 用户资料变更通知

```
            UserModifyPack userModifyPack = new UserModifyPack();
            BeanUtils.copyProperties(req,userModifyPack);
            messageProducer.sendToUser(req.getUserId(), req.getClientType(),req.getImei(), UserEventCommand.USER_MODIFY,userModifyPack,req.getAppId());
```

### 编写好友模块TCP通知

添加好友

```
  //通知
        //发送给from
        AddFriendPack addFriendPack = new AddFriendPack();
        BeanUtils.copyProperties(fromItem,addFriendPack);
       // addFriendPack.setSequence(seq);
        if(requestBase != null){
            messageProducer.sendToUser(fromId,requestBase.getClientType(),
                    requestBase.getImei(), FriendshipEventCommand.FRIEND_ADD,addFriendPack
                    ,requestBase.getAppId());
        }else {
            messageProducer.sendToUser(fromId,
                    FriendshipEventCommand.FRIEND_ADD,addFriendPack
                    ,requestBase.getAppId());
        }

        AddFriendPack addFriendToPack = new AddFriendPack();
        BeanUtils.copyProperties(toItem,addFriendPack);
        messageProducer.sendToUser(toItem.getFromId(),
                FriendshipEventCommand.FRIEND_ADD,addFriendToPack
                ,requestBase.getAppId());
```

更新

```
 if(responseVO.isOk()){
            UpdateFriendPack updateFriendPack = new UpdateFriendPack();
            updateFriendPack.setRemark(req.getToItem().getRemark());
            updateFriendPack.setToId(req.getToItem().getToId());
            messageProducer.sendToUser(req.getFromId(),
                    req.getClientType(),req.getImei(),FriendshipEventCommand
                            .FRIEND_UPDATE,updateFriendPack,req.getAppId());

            if (appConfig.isModifyFriendAfterCallback()) {
                AddFriendAfterCallbackDto callbackDto = new AddFriendAfterCallbackDto();
                callbackDto.setFromId(req.getFromId());
                callbackDto.setToItem(req.getToItem());
                callbackService.beforeCallback(req.getAppId(),
                        Constants.CallbackCommand.UpdateFriendAfter, JSONObject
                                .toJSONString(callbackDto));
            }
        }
```

### 封装GroupMessageProducer给群组用户发

```
    @Autowired
    MessageProducer messageProducer;

    @Autowired
    ImGroupMemberService imGroupMemberService;

    public void producer(String userId, Command command, Object data,
                         ClientInfo clientInfo){
        JSONObject o = (JSONObject) JSONObject.toJSON(data);
        String groupId = o.getString("groupId");
        List<String> groupMemberId = imGroupMemberService
                .getGroupMemberId(groupId, clientInfo.getAppId());

for (String memberId : groupMemberId) {
                if(clientInfo.getClientType() != null && clientInfo.getClientType() !=
                        ClientType.WEBAPI.getCode() && memberId.equals(userId)){
                    messageProducer.sendToUserExceptClient(memberId,command,
                            data,clientInfo);
                }else{
                    messageProducer.sendToUser(memberId,command,data,clientInfo.getAppId());
                }
            }
```

### 编写群组模块TCP通知







### TCP服务处理逻辑层投递的MQ消息

```
  new DefaultConsumer(channel) {
                        @Override
                        public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                            //处理服务端消息
                            try {
                                String msgStr = new String(body);
                                log.info(msgStr);
                                MessagePack messagePack =
                                        JSONObject.parseObject(msgStr, MessagePack.class);
                                BaseProcess messageProcess = ProcessFactory
                                        .getMessageProcess(messagePack.getCommand());
                                messageProcess.process(messagePack);
                                //这条消息的标记符，是否批量
                                channel.basicAck(envelope.getDeliveryTag(),false);

                            }catch (Exception e){
                                e.printStackTrace();
                                //不重回队列
                                channel.basicNack(envelope.getDeliveryTag(),false,false);
                            }
                        }
                    }
```

### 接口调用鉴权加密-加解密算法HMAC-SHA256

```
    public static void main(String[] args) throws InterruptedException {
        SigAPI asd = new SigAPI(10000, "123456");
        String sign = asd.genUserSig("lld", 100000000);
//        Thread.sleep(2000L);
        JSONObject jsonObject = decodeUserSig(sign);
        System.out.println("sign:" + sign);
        System.out.println("decoder:" + jsonObject.toString());
    }
```

### 接口调用鉴权加密-统一入口拦截器加密校验用户请求

```
package com.lld.im.service.interceptor;

import com.alibaba.fastjson.JSONObject;
import com.lld.im.common.BaseErrorCode;
import com.lld.im.common.ResponseVO;
import com.lld.im.common.config.AppConfig;
import com.lld.im.common.constant.Constants;
import com.lld.im.common.enums.GateWayErrorCode;
import com.lld.im.common.enums.ImUserTypeEnum;
import com.lld.im.common.exception.ApplicationExceptionEnum;
import com.lld.im.common.utils.SigAPI;
import com.lld.im.service.user.dao.ImUserDataEntity;
import com.lld.im.service.user.service.ImUserService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import sun.rmi.runtime.Log;

import java.util.concurrent.TimeUnit;

/**
 * @description:
 * @author: lld
 * @version: 1.0
 */
@Component
public class IdentityCheck {

    private static Logger logger = LoggerFactory.getLogger(IdentityCheck.class);

    @Autowired
    ImUserService imUserService;

    //10000 123456 10001 123456789
    @Autowired
    AppConfig appConfig;

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    public ApplicationExceptionEnum checkUserSig(String identifier,
                                                 String appId,String userSig){

        String cacheUserSig = stringRedisTemplate.opsForValue()
                .get(appId + ":" + Constants.RedisConstants.userSign + ":"
                + identifier + userSig);
        if(!StringUtils.isBlank(cacheUserSig) && Long.valueOf(cacheUserSig)
         >  System.currentTimeMillis() / 1000){
            this.setIsAdmin(identifier,Integer.valueOf(appId));
            return BaseErrorCode.SUCCESS;
        }

        //获取秘钥
        String privateKey = appConfig.getPrivateKey();

        //根据appid + 秘钥创建sigApi
        SigAPI sigAPI = new SigAPI(Long.valueOf(appId), privateKey);

        //调用sigApi对userSig解密
        JSONObject jsonObject = sigAPI.decodeUserSig(userSig);

        //取出解密后的appid 和 操作人 和 过期时间做匹配，不通过则提示错误
        Long expireTime = 0L;
        Long expireSec = 0L;
        Long time = 0L;
        String decoerAppId = "";
        String decoderidentifier = "";

        try {
            decoerAppId = jsonObject.getString("TLS.appId");
            decoderidentifier = jsonObject.getString("TLS.identifier");
            String expireStr = jsonObject.get("TLS.expire").toString();
            String expireTimeStr = jsonObject.get("TLS.expireTime").toString();
            time = Long.valueOf(expireTimeStr);
            expireSec = Long.valueOf(expireStr);
            expireTime = Long.valueOf(expireTimeStr) + expireSec;
        }catch (Exception e){
            e.printStackTrace();
            logger.error("checkUserSig-error:{}",e.getMessage());
        }

        if(!decoderidentifier.equals(identifier)){
            return GateWayErrorCode.USERSIGN_OPERATE_NOT_MATE;
        }

        if(!decoerAppId.equals(appId)){
            return GateWayErrorCode.USERSIGN_IS_ERROR;
        }

        if(expireSec == 0L){
            return GateWayErrorCode.USERSIGN_IS_EXPIRED;
        }

        if(expireTime < System.currentTimeMillis() / 1000){
            return GateWayErrorCode.USERSIGN_IS_EXPIRED;
        }

        //appid + "xxx" + userId + sign
        String genSig = sigAPI.genUserSig(identifier, expireSec,time,null);
        if (genSig.toLowerCase().equals(userSig.toLowerCase()))
        {
            String key = appId + ":" + Constants.RedisConstants.userSign + ":"
                    +identifier + userSig;

            Long etime = expireTime - System.currentTimeMillis() / 1000;
            stringRedisTemplate.opsForValue().set(
                    key,expireTime.toString(),etime, TimeUnit.SECONDS
            );
            this.setIsAdmin(identifier,Integer.valueOf(appId));
            return BaseErrorCode.SUCCESS;
        }

        return GateWayErrorCode.USERSIGN_IS_ERROR;
    }




```

```
package com.lld.im.service.interceptor;

import com.alibaba.fastjson.JSONObject;
import com.lld.im.common.BaseErrorCode;
import com.lld.im.common.ResponseVO;
import com.lld.im.common.enums.GateWayErrorCode;
import com.lld.im.common.exception.ApplicationExceptionEnum;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;

/**
 * @作者：xie
 * @时间：2023/4/16 19:48
 */
@Component
public class GateWayInterceptor implements HandlerInterceptor {
    @Autowired
    IdentityCheck identityCheck;
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

//        if (1 == 1){
//            return true;
//        }

        //获取appId 操作人 userSign
        String appIdStr = request.getParameter("appId");
        if(StringUtils.isBlank(appIdStr)){
            resp(ResponseVO.errorResponse(GateWayErrorCode
                    .APPID_NOT_EXIST),response);
            return false;
        }

        String identifier = request.getParameter("identifier");
        if(StringUtils.isBlank(identifier)){
            resp(ResponseVO.errorResponse(GateWayErrorCode
                    .OPERATER_NOT_EXIST),response);
            return false;
        }

        String userSign = request.getParameter("userSign");
        if(StringUtils.isBlank(userSign)){
            resp(ResponseVO.errorResponse(GateWayErrorCode
                    .USERSIGN_NOT_EXIST),response);
            return false;
        }

        //签名和操作人和appid是否匹配
        ApplicationExceptionEnum applicationExceptionEnum = identityCheck.checkUserSig(identifier, appIdStr, userSign);
        if(applicationExceptionEnum != BaseErrorCode.SUCCESS){
            resp(ResponseVO.errorResponse(applicationExceptionEnum),response);
            return false;
        }

        return true;
    }

    private void resp(ResponseVO respVo ,HttpServletResponse response){

        PrintWriter writer = null;
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/html; charset=utf-8");
        try {
            String resp = JSONObject.toJSONString(respVo);

            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-type", "application/json;charset=UTF-8");
            response.setHeader("Access-Control-Allow-Origin","*");
            response.setHeader("Access-Control-Allow-Credentials","true");
            response.setHeader("Access-Control-Allow-Methods","*");
            response.setHeader("Access-Control-Allow-Headers","*");
            response.setHeader("Access-Control-Max-Age","3600");

            writer = response.getWriter();
            writer.write(resp);
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            if(writer != null){
                writer.checkError();
            }
        }

    }
}

```

```
package com.lld.im.service.config;

import com.lld.im.service.interceptor.GateWayInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @description:
 * @author: lld
 * @version: 1.0
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    GateWayInterceptor gateWayInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(gateWayInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/v1/user/login")
                .excludePathPatterns("/v1/message/checkSend");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "HEAD", "POST", "PUT", "DELETE", "OPTIONS")
                .allowCredentials(true)
                .maxAge(3600)
                .allowedHeaders("*");
    }

}

```

