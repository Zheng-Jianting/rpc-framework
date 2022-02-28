package com.zhengjianting.zookeeper;

import com.zhengjianting.dto.RpcRequest;
import com.zhengjianting.provider.extension.SPI;

import java.net.InetSocketAddress;

@SPI
public interface ServiceDiscovery {
    InetSocketAddress lookupService(RpcRequest rpcRequest);
}