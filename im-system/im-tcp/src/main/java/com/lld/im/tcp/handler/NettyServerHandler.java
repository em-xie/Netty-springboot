package com.lld.im.tcp.handler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.lld.im.codec.pack.LoginPack;
import com.lld.im.codec.pack.message.ChatMessageAck;
import com.lld.im.codec.proto.Message;
import com.lld.im.codec.proto.MessagePack;
import com.lld.im.common.ResponseVO;
import com.lld.im.common.constant.Constants;
import com.lld.im.common.enums.ImConnectStatusEnum;
import com.lld.im.common.enums.command.GroupEventCommand;
import com.lld.im.common.enums.command.MessageCommand;
import com.lld.im.common.enums.command.SystemCommand;
import com.lld.im.common.model.UserClientDto;
import com.lld.im.common.model.UserSession;
import com.lld.im.common.model.message.CheckSendMessageReq;
import com.lld.im.tcp.feign.FeignMessageService;
import com.lld.im.tcp.publish.MqMessageProducer;
import com.lld.im.tcp.redis.RedisManger;
import com.lld.im.tcp.service.LimServer;
import com.lld.im.tcp.utils.SessionSocketHolder;
import feign.Feign;
import feign.Request;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
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
    private String logicUrl;

    public NettyServerHandler(Integer brokerId,String logicUrl) {
        this.brokerId = brokerId;
        feignMessageService = Feign.builder()
                .encoder(new JacksonEncoder())
                .decoder(new JacksonDecoder())
                .options(new Request.Options(1000, 3500))//设置超时时间
                .target(FeignMessageService.class, logicUrl);
    }

    private FeignMessageService feignMessageService;

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
            userSession.setImei(message.getMessageHeader().getImei());
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
                    (NioSocketChannel) channelHandlerContext.channel());

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

        } else if(command == MessageCommand.MSG_P2P.getCommand()
                || command == GroupEventCommand.MSG_GROUP.getCommand()) {
            try {
                String toId = "";
                CheckSendMessageReq req = new CheckSendMessageReq();
                req.setAppId(message.getMessageHeader().getAppId());
                req.setCommand(message.getMessageHeader().getCommand());
                JSONObject jsonObject = JSON.parseObject(JSONObject.toJSONString(message.getMessagePack()));
                String fromId = jsonObject.getString("fromId");
                if(command == MessageCommand.MSG_P2P.getCommand()){
                    toId = jsonObject.getString("toId");
                }else {
                    toId = jsonObject.getString("groupId");
                }
                req.setToId(toId);
                req.setFromId(fromId);

                ResponseVO responseVO = feignMessageService.checkSendMessage(req);
                if(responseVO.isOk()){
                    MqMessageProducer.sendMessage(message,command);
                }else{
                    Integer ackCommand = 0;
                    if(command == MessageCommand.MSG_P2P.getCommand()){
                        ackCommand = MessageCommand.MSG_ACK.getCommand();
                    }else {
                        ackCommand = GroupEventCommand.GROUP_MSG_ACK.getCommand();
                    }

                    ChatMessageAck chatMessageAck = new ChatMessageAck(jsonObject.getString("messageId"));
                    responseVO.setData(chatMessageAck);
                    MessagePack<ResponseVO> ack = new MessagePack<>();
                    ack.setData(responseVO);
                    ack.setCommand(ackCommand);
                    channelHandlerContext.channel().writeAndFlush(ack);
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }else {
            MqMessageProducer.sendMessage(message,command);
        }
    }
}
