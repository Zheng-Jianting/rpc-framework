package com.zhengjianting.example;

import com.zhengjianting.config.RpcServiceConfig;
import com.zhengjianting.example.impl.GoodByeServiceImpl;
import com.zhengjianting.example.impl.HelloServiceImpl;
import com.zhengjianting.service.GoodByeService;
import com.zhengjianting.service.HelloService;
import com.zhengjianting.transport.RpcNIOServer;

public class ServerExample {
    public static void main(String[] args)  {
        RpcNIOServer rpcNIOServer = new RpcNIOServer();

        // register hello service
        RpcServiceConfig rpcServiceConfig1 = new RpcServiceConfig();
        HelloService rpcHelloService = new HelloServiceImpl();
        rpcServiceConfig1.setService(rpcHelloService);
        rpcNIOServer.registerService(rpcServiceConfig1);

        // register goodBye service
        RpcServiceConfig rpcServiceConfig2 = new RpcServiceConfig();
        GoodByeService rpcGoodByeService = new GoodByeServiceImpl();
        rpcServiceConfig2.setService(rpcGoodByeService);
        rpcNIOServer.registerService(rpcServiceConfig2);

        // start server
        rpcNIOServer.start();
    }
}