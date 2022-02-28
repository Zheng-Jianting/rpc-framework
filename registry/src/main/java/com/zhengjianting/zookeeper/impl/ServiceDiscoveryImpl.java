package com.zhengjianting.zookeeper.impl;

import com.zhengjianting.dto.RpcRequest;
import com.zhengjianting.zookeeper.ServiceDiscovery;
import com.zhengjianting.zookeeper.util.CuratorUtils;
import org.apache.curator.framework.CuratorFramework;

import java.net.InetSocketAddress;
import java.util.List;

public class ServiceDiscoveryImpl implements ServiceDiscovery {
    @Override
    public InetSocketAddress lookupService(RpcRequest rpcRequest) {
        String rpcServiceName = rpcRequest.getRpcServiceName();
        CuratorFramework zkClient = CuratorUtils.getZkClient();
        List<String> addresses = CuratorUtils.getChildrenNodes(zkClient, rpcServiceName);
        if (addresses == null || addresses.size() == 0) {
            throw new RuntimeException("No server implementing this service was found.");
        }

        // default select the first address
        String host = addresses.get(0).split(":")[0];
        int port = Integer.parseInt(addresses.get(0).split(":")[1]);

        return new InetSocketAddress(host, port);
    }
}
