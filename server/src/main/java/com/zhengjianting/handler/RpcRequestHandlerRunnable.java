package com.zhengjianting.handler;

import com.zhengjianting.dto.RpcRequest;
import com.zhengjianting.dto.RpcResponse;
import com.zhengjianting.factory.SingletonFactory;
import com.zhengjianting.serialize.Serializer;
import com.zhengjianting.serialize.impl.ProtobufSerializerImpl;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.ByteBuffer;
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
            // read rpcRequest
            ByteBuffer buff = ByteBuffer.allocate(1024);
            byte[] requestBuff = new byte[0];

            while (true) {
                buff.clear();
                int len = socketChannel.read(buff);
                if (len == -1)
                    break;
                buff.flip(); // 由写切换为读, 即 limit = position; position = 0;

                int k = buff.limit();
                byte[] readBuff = new byte[k];
                buff.get(readBuff);

                byte[] tempBuff = new byte[requestBuff.length + k]; // temp buffer size = bytes already read + bytes last read
                System.arraycopy(requestBuff, 0, tempBuff, 0, requestBuff.length); // copy previous bytes
                System.arraycopy(readBuff, 0, tempBuff, requestBuff.length, k);  // copy current lot
                requestBuff = tempBuff; // call the temp buffer as your result buff

                buff.clear();
            }

            // serializer
            Serializer serializer = new ProtobufSerializerImpl();

            // invoke target method
            RpcRequest rpcRequest = serializer.deserialize(requestBuff, RpcRequest.class);
            log.info("deserialize rpcRequest: " + rpcRequest.getRpcServiceName());
            Object invokeResult = rpcRequestHandler.handler(rpcRequest);

            // write response
            RpcResponse<Object> rpcResponse = RpcResponse.success(invokeResult, rpcRequest.getRequestId());
            byte[] response = serializer.serialize(rpcResponse);
            socketChannel.write(ByteBuffer.wrap(response));

            // close socket channel
            socketChannel.close();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException("error from RpcRequestHandlerRunnable.");
        }
    }
}