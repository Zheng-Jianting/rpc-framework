package com.zhengjianting.example;

import com.zhengjianting.config.RpcServiceConfig;
import com.zhengjianting.proxy.RpcClientProxy;
import com.zhengjianting.service.GoodByeService;
import com.zhengjianting.service.HelloService;
import com.zhengjianting.transport.impl.RpcRequestTransportImpl;

public class ClientExample {
    public static void main(String[] args) throws InterruptedException {
        // get client proxy
        RpcClientProxy rpcClientProxy = new RpcClientProxy(new RpcServiceConfig(), new RpcRequestTransportImpl());

        // get HelloService proxy
        HelloService helloServiceProxy = rpcClientProxy.getProxy(HelloService.class);
        String helloResult = helloServiceProxy.hello("zjt");
        System.out.println(helloResult);

        // get GoodByeService proxy
        GoodByeService goodByeServiceProxy = rpcClientProxy.getProxy(GoodByeService.class);
        String goodByeResult = goodByeServiceProxy.goodBye("zjt");
        System.out.println(goodByeResult);

        synchronized (ClientExample.class) {
            ClientExample.class.wait();
        }
    }
}