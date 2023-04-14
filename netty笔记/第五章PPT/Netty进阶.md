IM开发核心之构建TCP网关（上）使用netty实现聊天室+编码器

```
try {
            ServerBootstrap b = new ServerBootstrap(); // (2)
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class) // (3)
                    .childHandler(new ChannelInitializer<SocketChannel>() { // (4)
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            Charset gbk = Charset.forName("utf-8");
                            ch.pipeline().addLast(new DelimiterBasedFrameDecoder(1024, Unpooled.copiedBuffer("_"
                                    .getBytes())));
                            ch.pipeline().addLast("encoder", new StringEncoder(gbk));//out
                            ch.pipeline().addLast("decoder", new StringDecoder(gbk));//in
                            ch.pipeline().addLast(new DiscardServerHandler());
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)          // (5)
                    .childOption(ChannelOption.SO_KEEPALIVE, true); // (6)

            // Bind and start to accept incoming connections.
            System.out.println("tcp start success");
            ChannelFuture f = b.bind(port).sync(); // (7)


            // Wait until the server socket is closed.
            // In this example, this does not happen, but you can do that to gracefully
            // shut down your server.
            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
```

```
package netty.chat.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.HashSet;
import java.util.Set;

/**
 * @description:
 * @author: lld
 * @version: 1.0
 */
public class DiscardServerHandler  extends ChannelInboundHandlerAdapter {

    static Set<Channel> channelList = new HashSet<>();

//    @Override
//    public void channelActive(ChannelHandlerContext ctx) throws Exception {
//        //通知其他人 我上线了
//        channelList.forEach(e->{
//            e.writeAndFlush("[客户端]" + ctx.channel().remoteAddress() + "上线了");
//        });
//
//        channelList.add(ctx.channel());
//    }

    //网络调试助手 -> 服务端
    //？直接发送！？
    //网络调试助手 -> 操作系统 -> 网络 -> 对方操作系统 -> 9000找到对应进程（我们的服务端）
    //!字符串
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        String message = (String) msg;
//        Charset gbk = Charset.forName("GBK");
        System.out.println("收到数据：" + message);
//        分发给聊天室内的所有客户端
//        通知其他人 我上线了
        channelList.forEach(e->{
            if(e == ctx.channel()){
                e.writeAndFlush("[自己] ： " + message);
            }else{
                e.writeAndFlush("[客户端] " +ctx.channel().remoteAddress()+"：" + message);
            }
        });


    }

    /**
     * @description: channel 处于不活跃的时候会调用
     * @param
     * @return void
     * @author lld
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        //通知其他客户端 我下线了
        channelList.remove(ctx.channel());
        //通知其他人 我上线了
        channelList.forEach(e->{
            e.writeAndFlush("[客户端]" + ctx.channel().remoteAddress() + "下线了");
        });
    }
}

```

![image-20230413163914034](E:\java2\netty-springboot\netty笔记\第五章PPT\image-20230413163914034.png)

msg -》 head - 》 StringDecoder(in) -》 ChatServerHandler -》 StringEncoder（out）





### netty 解决粘包

```
ch.pipeline().addLast(new FixedLengthFrameDecoder(8));
```

```
           //下划线                 ch.pipeline().addLast(new DelimiterBasedFrameDecoder(1024, Unpooled.copiedBuffer("_"
                                    .getBytes())));
```

utf8 中文 3

gbk 中文 2



### 自定义私有协议

```
                            Charset gbk = Charset.forName("utf-8");
                            ch.pipeline().addLast(new MyDecodecer());
```

```
package netty.my.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * @description:
 * @author: lld
 * @version: 1.0
 */
public class MyDecodecer extends ByteToMessageDecoder {


    //数据长度 + 数据
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if(in.readableBytes() < 4){
            return;
        }
        //数据长度 4 + 10000  9999
        int i = in.readInt();
        if(in.readableBytes() < i){
            in.resetReaderIndex();
            return;
        }
        byte[] data = new byte[i];//10000
        in.readBytes(data);
        System.out.println(new String(data));
        in.markReaderIndex();//10004
    }
}

```

### NettyByteBuf核心api

```
package netty.bytebuf;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class NettyByteBuf {
    public static void main(String[] args) {
        // 创建byteBuf对象，该对象内部包含一个字节数组byte[10]
        ByteBuf byteBuf = Unpooled.buffer(10);
        System.out.println("byteBuf=" + byteBuf);

        for (int i = 0; i < 8; i++) {
            byteBuf.writeByte(i);
        }
        System.out.println("byteBuf=" + byteBuf);

        for (int i = 0; i < 5; i++) {
            System.out.println(byteBuf.getByte(i));
        }
        System.out.println("byteBuf=" + byteBuf);

        for (int i = 0; i < 5; i++) {
            System.out.println(byteBuf.readByte());
        }
        System.out.println("byteBuf=" + byteBuf);

//        byteBuf.readableBytes();  剩余索引
//        byteBuf.markReaderIndex(); 标记索引
    }


}

```

### HeartbeatHandler心跳机制

```
          //超时读，写，全部                  ch.pipeline().addLast(new IdleStateHandler(3,0,0));
                            ch.pipeline().addLast(new HeartbeatHandler());
```

```
package netty.heaetbeat.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

/**
 * @description:
 * @author: lld
 * @version: 1.0
 */
public class HeartbeatHandler extends ChannelInboundHandlerAdapter {

    int readTimeOut = 0;

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        IdleStateEvent event = (IdleStateEvent) evt;

        if(event.state() == IdleState.READER_IDLE){
            readTimeOut++;
        }

        if(readTimeOut >= 3){
            System.out.println("超时超过3次，断开连接");
            ctx.close();
        }

//        System.out.println("触发了：" + event.state() + "事件");
    }
}

```

