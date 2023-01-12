package com.ab;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Hello world!
 */
public class App {


    public static void main(String[] args) throws InterruptedException {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            ChannelPipeline pipeline = socketChannel.pipeline();
                            pipeline.addLast(new LoggingHandler(LogLevel.INFO))
                                    .addLast(new ChatNettyHandler());
                        }
                    });

            ChannelFuture f = serverBootstrap.bind(5555).sync();
            f.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }


    }

    private static class ChatNettyHandler extends SimpleChannelInboundHandler<ByteBuf> {

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            System.out.println("one conn active:" + ctx.channel());
            ChatHolder.join((SocketChannel) ctx.channel());
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, ByteBuf byteBuf) throws Exception {
            byte[] bytes = new byte[byteBuf.readableBytes()];
            byteBuf.readBytes(bytes);
            String content = new String(bytes, StandardCharsets.UTF_8);
            System.out.println(content);
            if (content.equals("quit\r\n")) {
                ctx.close();
            } else {
                ChatHolder.propagate((SocketChannel) ctx.channel(), content);
            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            System.out.println("one conn inactive: " + ctx.channel());
            ChatHolder.quit((SocketChannel) ctx.channel());
        }
    }

    private static class ChatHolder {
        static final Map<SocketChannel, String> USER_MAP = new ConcurrentHashMap<>();

        /**
         * 加入群聊
         *
         * @param socketChannel
         */
        static void join(SocketChannel socketChannel) {
            String userId = "user" + ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE);
            send(socketChannel, "your id is " + userId + "\r\n");
            for (SocketChannel channel : USER_MAP.keySet()) {
                send(channel, userId + " join chat \r\n");
            }
            //将当前用户加入map中
            USER_MAP.put(socketChannel, userId);
        }

        static void quit(SocketChannel socketChannel) {
            String userId = USER_MAP.get(socketChannel);
            send(socketChannel, "you quit chat.\r\n");
            USER_MAP.remove(socketChannel);
            for (SocketChannel channel : USER_MAP.keySet()) {
                if (channel != socketChannel) {
                    send(channel, userId + " quit chat.\r\n");
                }
            }
        }

        public static void propagate(SocketChannel socketChannel, String content) {
            String userId = USER_MAP.get(socketChannel);
            for (SocketChannel channel : USER_MAP.keySet()) {
                if (channel != socketChannel) {
                    send(channel, userId + ":" + content);
                }
            }

        }

        static void send(SocketChannel socketChannel, String msg) {
            try {
                ByteBufAllocator allocator = ByteBufAllocator.DEFAULT;
                ByteBuf writeBuffer = allocator.buffer(msg.getBytes().length);
                writeBuffer.writeCharSequence(msg, Charset.defaultCharset());
                socketChannel.writeAndFlush(writeBuffer);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
