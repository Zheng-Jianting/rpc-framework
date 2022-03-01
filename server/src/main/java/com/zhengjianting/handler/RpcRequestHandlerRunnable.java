package com.zhengjianting.handler;

import com.zhengjianting.dto.RpcRequest;
import com.zhengjianting.dto.RpcResponse;
import com.zhengjianting.factory.SingletonFactory;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.channels.SocketChannel;

@Slf4j
public class RpcRequestHandlerRunnable implements Runnable {
    private final SocketChannel socketChannel;
    private final RpcRequestHandler rpcRequestHandler;

    public RpcRequestHandlerRunnable(SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
        this.rpcRequestHandler = SingletonFactory.getInstance(RpcRequestHandler.class);
    }

    @Override
    public void run() {
        try {
            ObjectInputStream in = new ObjectInputStream(socketChannel.socket().getInputStream());
            ObjectOutputStream out = new ObjectOutputStream(socketChannel.socket().getOutputStream());
            // read rpcRequest
            RpcRequest rpcRequest = (RpcRequest) in.readObject();
            // invoke target method
            Object invokeResult = rpcRequestHandler.handler(rpcRequest);
            // write response
            out.writeObject(RpcResponse.success(invokeResult, rpcRequest.getRequestId()));
            out.flush();
        } catch (IOException | ClassNotFoundException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException("error from RpcRequestHandlerRunnable.");
        }
    }
}