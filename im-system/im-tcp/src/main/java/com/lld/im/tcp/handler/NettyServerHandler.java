package com.lld.im.tcp.handler;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.lld.im.codec.pack.LoginPack;
import com.lld.im.codec.proto.Message;
import com.lld.im.common.constant.Constants;
import com.lld.im.common.enums.ImConnectStatusEnum;
import com.lld.im.common.enums.command.SystemCommand;
import com.lld.im.common.model.UserClientDto;
import com.lld.im.common.model.UserSession;
import com.lld.im.tcp.redis.RedisManger;
import com.lld.im.tcp.service.LimServer;
import com.lld.im.tcp.utils.SessionSocketHolder;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.AttributeKey;
import org.redisson.api.RMap;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;

/**
 * @作者：xie
 * @时间：2023/4/14 13:27
 */
public class NettyServerHandler extends SimpleChannelInboundHandler<Message> {
    private final static Logger logger = LoggerFactory.getLogger(LimServer.class);

    private Integer brokerId;

    public NettyServerHandler(Integer brokerId) {
        this.brokerId = brokerId;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Message message) throws Exception {
        Integer command = message.getMessageHeader().getCommand();
        if(command== SystemCommand.LOGIN.getCommand())
        {
            LoginPack loginPack = JSONObject.parseObject(JSONObject.toJSONString(message.getMessagePack())
                    , new TypeReference<LoginPack>() {
                    }.getType());

            channelHandlerContext.channel().attr(AttributeKey.valueOf(Constants.UserId)).set(loginPack.getUserId());
            channelHandlerContext.channel().attr(AttributeKey.valueOf(Constants.AppId)).set(message.getMessageHeader().getAppId());
            channelHandlerContext.channel().attr(AttributeKey.valueOf(Constants.ClientType)).set(message.getMessageHeader().getClientType());
            channelHandlerContext.channel().attr(AttributeKey.valueOf(Constants.Imei)).set(message.getMessageHeader().getImei());
            //Redis map

            UserSession userSession = new UserSession();
            userSession.setAppId(message.getMessageHeader().getAppId());
            userSession.setClientType(message.getMessageHeader().getClientType());
            userSession.setUserId(loginPack.getUserId());
            userSession.setConnectState(ImConnectStatusEnum.ONLINE_STATUS.getCode());
            userSession.setBrokerId(brokerId);
//            userSession.setImei(msg.getMessageHeader().getImei());
            try{
                InetAddress localHost = InetAddress.getLocalHost();
                userSession.setBrokerHost(localHost.getHostAddress());
            }catch (Exception e){
                e.printStackTrace();
            }

            //存进redis
            RedissonClient redissonClient = RedisManger.getRedissonClient();
            RMap<String, String> map = redissonClient.getMap(message.getMessageHeader().getAppId() + Constants.RedisConstants.UserSessionConstants + loginPack.getUserId());
            map.put(message.getMessageHeader().getClientType()+":"+message.getMessageHeader().getImei() ,JSONObject.toJSONString(userSession));

            //将channel存起来
            SessionSocketHolder.put(message.getMessageHeader().getAppId(),
                    loginPack.getUserId(),message.getMessageHeader().getClientType(),
                    message.getMessageHeader().getImei(),
                    (NioSocketChannel) channelHandlerContext);

            //发送客户端消息
            UserClientDto userClientDto = new UserClientDto();
            userClientDto.setImei(message.getMessageHeader().getImei());
            userClientDto.setClientType(message.getMessageHeader().getClientType());
            userClientDto.setUserId(loginPack.getUserId());
            userClientDto.setAppId(message.getMessageHeader().getAppId());
            RTopic topic = redissonClient.getTopic(Constants.RedisConstants.UserLoginChannel);
            topic.publish(JSONObject.toJSONString(userClientDto));

        } else if (command== SystemCommand.LOGOUT.getCommand()) {
            //删除session 与 redis
            SessionSocketHolder.removeUserSession((NioSocketChannel) channelHandlerContext.channel() );
        } else if (command== SystemCommand.PING.getCommand()) {
           channelHandlerContext.channel().attr(AttributeKey.valueOf(Constants.ReadTime)).set(System.currentTimeMillis());

        }
    }
}
