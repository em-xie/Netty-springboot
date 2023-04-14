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
