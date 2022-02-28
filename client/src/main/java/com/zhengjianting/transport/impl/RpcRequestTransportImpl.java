package com.zhengjianting.transport.impl;

import com.zhengjianting.dto.RpcRequest;
import com.zhengjianting.provider.extension.ExtensionLoader;
import com.zhengjianting.transport.RpcRequestTransport;
import com.zhengjianting.zookeeper.ServiceDiscovery;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
            socket.connect(address);

            // send request to server
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            out.writeObject(rpcRequest);

            // receive response from server
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            return in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException("error from sendRpcRequest.");
        }
    }
}
