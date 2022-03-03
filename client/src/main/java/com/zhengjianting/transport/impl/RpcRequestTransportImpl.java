package com.zhengjianting.transport.impl;

import com.zhengjianting.dto.RpcRequest;
import com.zhengjianting.dto.RpcResponse;
import com.zhengjianting.provider.extension.ExtensionLoader;
import com.zhengjianting.serialize.Serializer;
import com.zhengjianting.serialize.impl.ProtobufSerializerImpl;
import com.zhengjianting.transport.RpcRequestTransport;
import com.zhengjianting.zookeeper.ServiceDiscovery;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;

@Slf4j
public class RpcRequestTransportImpl implements RpcRequestTransport {
    private final ServiceDiscovery serviceDiscovery;

    public RpcRequestTransportImpl() {
        this.serviceDiscovery = ExtensionLoader.getExtensionLoader(ServiceDiscovery.class).getExtension("zookeeper");
    }

    @Override
    public Object sendRpcRequest(RpcRequest rpcRequest) {
        InetSocketAddress address = serviceDiscovery.lookupService(rpcRequest);
        try {
            Socket socket = new Socket();
            log.info(address.toString());
            socket.connect(address);

            // serializer
            Serializer serializer = new ProtobufSerializerImpl();

            // send request to server
            OutputStream out = socket.getOutputStream();
            out.write(serializer.serialize(rpcRequest));
            out.flush();
            socket.shutdownOutput();

            // receive response from server
            InputStream in = socket.getInputStream();
            byte[] responseBuff = new byte[0];
            byte[] buff = new byte[1024];
            int k = -1;
            while((k = in.read(buff, 0, buff.length)) > -1) {
                byte[] tempBuff = new byte[responseBuff.length + k]; // temp buffer size = bytes already read + bytes last read
                System.arraycopy(responseBuff, 0, tempBuff, 0, responseBuff.length); // copy previous bytes
                System.arraycopy(buff, 0, tempBuff, responseBuff.length, k);  // copy current lot
                responseBuff = tempBuff; // call the temp buffer as your result buff
            }

            return serializer.deserialize(responseBuff, RpcResponse.class);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException("error from sendRpcRequest.");
        }
    }
}
