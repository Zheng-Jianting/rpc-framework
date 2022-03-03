package com.zhengjianting.transport;

import com.zhengjianting.config.RpcServiceConfig;
import com.zhengjianting.factory.SingletonFactory;
import com.zhengjianting.handler.RpcRequestHandlerRunnable;
import com.zhengjianting.provider.ServiceProvider;
import com.zhengjianting.provider.impl.ServiceProviderImpl;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RpcNIOServer {
    private final ServiceProvider serviceProvider;
    // thread pool
    private final ThreadPoolExecutor executor;
    private static final int CORE_POOL_SIZE = 5;
    private static final int MAX_POOL_SIZE = 10;
    private static final long KEEP_ALIVE_TIME = 1L;
    private static final int QUEUE_CAPACITY = 100;

    private final Set<SocketChannel> socketChannelSet;

    public RpcNIOServer() {
        serviceProvider = SingletonFactory.getInstance(ServiceProviderImpl.class);
        executor = new ThreadPoolExecutor(
                CORE_POOL_SIZE,
                MAX_POOL_SIZE,
                KEEP_ALIVE_TIME,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(QUEUE_CAPACITY),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        socketChannelSet = ConcurrentHashMap.newKeySet();
    }

    public void registerService(RpcServiceConfig rpcServiceConfig) {
        serviceProvider.publishService(rpcServiceConfig);
    }

    public void start() {
        try {
            Selector selector = Selector.open();

            ServerSocketChannel ssChannel = ServerSocketChannel.open();
            ssChannel.configureBlocking(false);
            ssChannel.register(selector, SelectionKey.OP_ACCEPT);

            ServerSocket serverSocket = ssChannel.socket();
            String localhost = InetAddress.getLocalHost().getHostAddress();
            serverSocket.bind(new InetSocketAddress(localhost, 10526));

            while (true) {
                /**
                 * This method performs a blocking selection operation.
                 * It returns only after at least one channel is selected.
                 */
                selector.select();
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> selectionKeyIterator = selectionKeys.iterator();

                while (selectionKeyIterator.hasNext()) {
                    SelectionKey selectionKey = selectionKeyIterator.next();

                    if (selectionKey.isValid() && selectionKey.isAcceptable()) {
                        /**
                         * 获取与事件 SelectionKey 相关联的通道 Channel
                         * 返回值类型为 SelectableChannel, 它是所有可以在选择器上注册的 Channel 的父类
                         */
                        ServerSocketChannel ssChannel1 = (ServerSocketChannel) selectionKey.channel();

                        // 服务器会为每个新连接创建一个 SocketChannel
                        SocketChannel socketChannel = ssChannel1.accept();
                        socketChannel.configureBlocking(false);

                        /**
                         * 这个 Socket Channel 主要用于从客户端读取数据
                         * 需要将其注册到选择器 Selector 上, 否则无法监听到达 sChannel 的数据
                         */
                        socketChannel.register(selector, SelectionKey.OP_READ);
                    }
                    else if (selectionKey.isValid() && selectionKey.isReadable()) {
                        log.info("selectionKey is readable.");

                        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();

                        /**
                         * 当 IO 事件准备好时, 新建一个线程处理
                         * 在这指客户端发送的 RpcRequest 经网络传输到达了服务端的内核缓冲区
                         */
                        if (!socketChannelSet.contains(socketChannel)) {
                            log.info("create new thread.");
                            socketChannelSet.add(socketChannel);
                            executor.execute(new RpcRequestHandlerRunnable(socketChannel));
                        }
                    }

                    // 处理完毕后就移除, 避免事件被重复处理
                    selectionKeyIterator.remove();
                }
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException("error from RpcNIOServer.");
        } finally {
            executor.shutdown();
        }
    }
}