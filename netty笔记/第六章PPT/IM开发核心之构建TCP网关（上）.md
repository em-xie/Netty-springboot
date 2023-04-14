### 正式开始编写IM主程序

```
package com.lld.im.tcp.service;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @作者：xie
 * @时间：2023/4/13 21:23
 */
public class LimServer {
    private final static Logger logger = LoggerFactory.getLogger(LimServer.class);

    private int port;

    public LimServer(int port){
        NioEventLoopGroup mainGroup = new NioEventLoopGroup();
        NioEventLoopGroup subGroup = new NioEventLoopGroup();
        ServerBootstrap server = new ServerBootstrap();
        server.group(mainGroup,subGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 10240) // 服务端可连接队列大小
                .option(ChannelOption.SO_REUSEADDR, true) // 参数表示允许重复使用本地地址和端口
                .childOption(ChannelOption.TCP_NODELAY, true) // 是否禁用Nagle算法 简单点说是否批量发送数据 true关闭 false开启。 开启的话可以减少一定的网络开销，但影响消息实时性
                .childOption(ChannelOption.SO_KEEPALIVE, true) // 保活开关2h没有数据服务端会发送心跳包
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {

                    }
                });
        server.bind(port);
    }
}

```

```
package com.lld.im.tcp;

import com.lld.im.tcp.service.LimServer;

/**
 * @作者：xie
 * @时间：2023/4/13 21:18
 */
public class Starter {

    public static void main(String[] args) {
        new LimServer(9000);
    }
}

```

### LimWebSocketServer配置

```
package com.lld.im.tcp.service;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @作者：xie
 * @时间：2023/4/13 21:48
 */
public class LimWebSocketServer {

    private final static Logger logger = LoggerFactory.getLogger(LimWebSocketServer.class);

    public LimWebSocketServer(int port){
        NioEventLoopGroup mainGroup = new NioEventLoopGroup();
        NioEventLoopGroup subGroup = new NioEventLoopGroup();
        ServerBootstrap server = new ServerBootstrap();
        server.group(mainGroup,subGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 10240) // 服务端可连接队列大小
                .option(ChannelOption.SO_REUSEADDR, true) // 参数表示允许重复使用本地地址和端口
                .childOption(ChannelOption.TCP_NODELAY, true) // 是否禁用Nagle算法 简单点说是否批量发送数据 true关闭 false开启。 开启的话可以减少一定的网络开销，但影响消息实时性
                .childOption(ChannelOption.SO_KEEPALIVE, true) // 保活开关2h没有数据服务端会发送心跳包
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        // websocket 基于http协议，所以要有http编解码器
                        pipeline.addLast("http-codec", new HttpServerCodec());
                        // 对写大数据流的支持
                        pipeline.addLast("http-chunked", new ChunkedWriteHandler());
                        // 几乎在netty中的编程，都会使用到此hanler
                        pipeline.addLast("aggregator", new HttpObjectAggregator(65535));
                        /**
                         * websocket 服务器处理的协议，用于指定给客户端连接访问的路由 : /ws
                         * 本handler会帮你处理一些繁重的复杂的事
                         * 会帮你处理握手动作： handshaking（close, ping, pong） ping + pong = 心跳
                         * 对于websocket来讲，都是以frames进行传输的，不同的数据类型对应的frames也不同
                         */
                        pipeline.addLast(new WebSocketServerProtocolHandler("/ws"));
                    }
                });
        server.bind(port);
        logger.info("11111111");
    }




}

```

### snakeyaml动态解析配置文件

[(72条消息) SnakeYAML配置文件解析器_snakeyaml 配置文件_四问四不知的博客-CSDN博客](https://blog.csdn.net/zkkzpp258/article/details/118345821)

```
lim:
  tcpPort: 9000
  webSocketPort: 19000
  bossThreadSize: 1
  workThreadSize: 8
```

```
@Data
public class BootstrapConfig {

    private TcpConfig lim;


    @Data
    public static class TcpConfig {
        private Integer tcpPort;// tcp 绑定的端口号

        private Integer webSocketPort; // webSocket 绑定的端口号

//        private boolean enableWebSocket; //是否启用webSocket

        private Integer bossThreadSize; // boss线程 默认=1

        private Integer workThreadSize; //work线程
        }
}
```

```
package com.lld.im.tcp.service;

import com.lld.im.codec.config.BootstrapConfig;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @作者：xie
 * @时间：2023/4/13 21:23
 */
public class LimServer {
    private final static Logger logger = LoggerFactory.getLogger(LimServer.class);
    BootstrapConfig.TcpConfig config;
    NioEventLoopGroup mainGroup;
    NioEventLoopGroup subGroup;
    ServerBootstrap server;

    public LimServer(BootstrapConfig.TcpConfig config){
        this.config = config;
        mainGroup = new NioEventLoopGroup(config.getBossThreadSize());
        subGroup = new NioEventLoopGroup(config.getWorkThreadSize());
        server = new ServerBootstrap();
        server.group(mainGroup,subGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 10240) // 服务端可连接队列大小
                .option(ChannelOption.SO_REUSEADDR, true) // 参数表示允许重复使用本地地址和端口
                .childOption(ChannelOption.TCP_NODELAY, true) // 是否禁用Nagle算法 简单点说是否批量发送数据 true关闭 false开启。 开启的话可以减少一定的网络开销，但影响消息实时性
                .childOption(ChannelOption.SO_KEEPALIVE, true) // 保活开关2h没有数据服务端会发送心跳包
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {

                    }
                });

    }
    public void start()
    {
        this.server.bind(this.config.getTcpPort());
    }
}

```

```
package com.lld.im.tcp;

import com.lld.im.codec.config.BootstrapConfig;
import com.lld.im.tcp.service.LimServer;
import com.lld.im.tcp.service.LimWebSocketServer;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * @作者：xie
 * @时间：2023/4/13 21:18
 */
public class Starter {

    public static void main(String[] args) throws FileNotFoundException {

        if(args.length > 0){
            start(args[0]);
        }
    }
    private static void start(String path){
        try {
            Yaml yaml = new Yaml();
            InputStream inputStream = new FileInputStream(path);
            BootstrapConfig bootstrapConfig = yaml.loadAs(inputStream, BootstrapConfig.class);

            new LimServer(bootstrapConfig.getLim()).start();
            new LimWebSocketServer(bootstrapConfig.getLim()).start();

        }catch (Exception e){
            e.printStackTrace();
            System.exit(500);
        }
    }
}

```

### 私有协议编解码设计

```
    //HTTP GET POST PUT DELETE 1.0 1.1 2.0
    //client IOS 安卓 pc(windows mac) web //支持json 也支持 protobuf
    //appId
    //28 + imei + body
    //请求头（指令 版本 clientType 消息解析类型 imei长度 appId bodylen）+ imei号 + 请求体
    //len+body
```

### 私有协议编解码实现

```
package com.lld.im.codec;

import com.lld.im.codec.proto.Message;
import com.lld.im.codec.utils.ByteBufToMessageUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;


import java.util.List;

/**
 * @作者：xie
 * @时间：2023/4/14 13:11
 */
public class MessageDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf in, List<Object> out) throws Exception {
        //请求头（指令
        // 版本
        // clientType
        // 消息解析类型
        // appId
        // imei长度
        // bodylen）+ imei号 + 请求体

        if(in.readableBytes() < 28){
            return;
        }

        Message message = ByteBufToMessageUtils.transition(in);
        if(message == null){
            return;
        }

        out.add(message);
    }
}

```

```
package com.lld.im.codec.utils;

import com.alibaba.fastjson.JSONObject;
import com.lld.im.codec.proto.Message;
import com.lld.im.codec.proto.MessageHeader;
import io.netty.buffer.ByteBuf;

/**
 * @author: Chackylee
 * @description: 将ByteBuf转化为Message实体，根据私有协议转换
 *               私有协议规则，
 *               4位表示Command表示消息的开始，
 *               4位表示version
 *               4位表示clientType
 *               4位表示messageType
 *               4位表示appId
 *               4位表示imei长度
 *               imei
 *               4位表示数据长度
 *               data
 *               后续将解码方式加到数据头根据不同的解码方式解码，如pb，json，现在用json字符串
 * @version: 1.0
 */
public class ByteBufToMessageUtils {

    public static Message transition(ByteBuf in){

        /** 获取command*/
        int command = in.readInt();

        /** 获取version*/
        int version = in.readInt();

        /** 获取clientType*/
        int clientType = in.readInt();

        /** 获取messageType*/
        int messageType = in.readInt();

        /** 获取appId*/
        int appId = in.readInt();

        /** 获取imeiLength*/
        int imeiLength = in.readInt();

        /** 获取bodyLen*/
        int bodyLen = in.readInt();

        if(in.readableBytes() < bodyLen + imeiLength){
            //重置读索引
            in.resetReaderIndex();
            return null;
        }

        byte [] imeiData = new byte[imeiLength];
        in.readBytes(imeiData);
        String imei = new String(imeiData);

        byte [] bodyData = new byte[bodyLen];
        in.readBytes(bodyData);


        MessageHeader messageHeader = new MessageHeader();
        messageHeader.setAppId(appId);
        messageHeader.setClientType(clientType);
        messageHeader.setCommand(command);
        messageHeader.setLength(bodyLen);
        messageHeader.setVersion(version);
        messageHeader.setMessageType(messageType);
        messageHeader.setImei(imei);

        Message message = new Message();
        message.setMessageHeader(messageHeader);

        if(messageType == 0x0){
            String body = new String(bodyData);
            JSONObject parse = (JSONObject) JSONObject.parse(body);
            message.setMessagePack(parse);
        }

        in.markReaderIndex();
        return message;
    }

}

```

```
package com.lld.im.codec.proto;

import lombok.Data;

/**
 * @作者：xie
 * @时间：2023/4/14 13:13
 */
@Data
public class MessageHeader {

    //消息操作指令 十六进制 一个消息的开始通常以0x开头
    //4字节
    private Integer command;
    //4字节 版本号
    private Integer version;
    //4字节 端类型
    private Integer clientType;
    /**
     * 应用ID
     */
//    4字节 appId
    private Integer appId;
    /**
     * 数据解析类型 和具体业务无关，后续根据解析类型解析data数据 0x0:Json,0x1:ProtoBuf,0x2:Xml,默认:0x0
     */
    //4字节 解析类型
    private Integer messageType = 0x0;

    //4字节 imel长度
    private Integer imeiLength;

    //4字节 包体长度
    private int length;

    //imei号
    private String imei;
}

```

```
package com.lld.im.codec.proto;

import lombok.Data;

/**
 * @作者：xie
 * @时间：2023/4/14 13:13
 */
@Data
public class Message {

    private MessageHeader messageHeader;

    private Object messagePack;

    @Override
    public String toString() {
        return "Message{" +
                "messageHeader=" + messageHeader +
                ", messagePack=" + messagePack +
                '}';
    }
}

```

```
package com.lld.im.codec.proto;

import lombok.Data;

import java.io.Serializable;

/**
 * @author: Chackylee
 * @description: 消息服务发送给tcp的包体,tcp再根据改包体解析成Message发给客户端
 **/
@Data
public class MessagePack<T> implements Serializable {

    private String userId;

    private Integer appId;

    /**
     * 接收方
     */
    private String toId;

    /**
     * 客户端标识
     */
    private int clientType;

    /**
     * 消息ID
     */
    private String messageId;

    /**
     * 客户端设备唯一标识
     */
    private String imei;

    private Integer command;

    /**
     * 业务数据对象，如果是聊天消息则不需要解析直接透传
     */
    private T data;

//    /** 用户签名*/
//    private String userSign;

}
```

```
package com.lld.im.tcp.handler;

import com.lld.im.codec.proto.Message;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * @作者：xie
 * @时间：2023/4/14 13:27
 */
public class NettyServerHandler extends SimpleChannelInboundHandler<Message> {
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Message message) throws Exception {
        System.out.printf(String.valueOf(message));
    }
}

```

```
                      ch.pipeline().addLast(new MessageDecoder());
                        ch.pipeline().addLast(new NettyServerHandler());
```

