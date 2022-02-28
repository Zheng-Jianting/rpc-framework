package com.zhengjianting.proxy;

import com.zhengjianting.config.RpcServiceConfig;
import com.zhengjianting.dto.RpcRequest;
import com.zhengjianting.transport.RpcRequestTransport;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;

@Slf4j
public class RpcClientProxy implements InvocationHandler {
    private final RpcServiceConfig rpcServiceConfig;
    private final RpcRequestTransport rpcRequestTransport;

    public RpcClientProxy(RpcServiceConfig rpcServiceConfig, RpcRequestTransport rpcRequestTransport) {
        this.rpcServiceConfig = rpcServiceConfig;
        this.rpcRequestTransport = rpcRequestTransport;
    }

    @SuppressWarnings("unchecked")
    public <T> T getProxy(Class<T> clazz) {
        return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[] { clazz }, this);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        RpcRequest rpcRequest = RpcRequest.builder().requestId(UUID.randomUUID().toString())
                                                    .methodName(method.getName())
                                                    .paramTypes(method.getParameterTypes())
                                                    .parameters(args)
                                                    .interfaceName(method.getDeclaringClass().getName())
                                                    .group(rpcServiceConfig.getGroup())
                                                    .version(rpcServiceConfig.getVersion())
                                                    .build();
        // return value should be casted to rpcResponse.
        return rpcRequestTransport.sendRpcRequest(rpcRequest);
    }
}