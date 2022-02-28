package com.zhengjianting.transport;

import com.zhengjianting.dto.RpcRequest;
import com.zhengjianting.provider.extension.SPI;

@SPI
public interface RpcRequestTransport {
    Object sendRpcRequest(RpcRequest rpcRequest);
}