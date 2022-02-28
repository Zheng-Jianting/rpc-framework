package com.zhengjianting.zookeeper;

import com.zhengjianting.provider.extension.SPI;

import java.net.InetSocketAddress;

@SPI
public interface ServiceRegistry {
    void registerService(String rpcServiceName, InetSocketAddress address);
}