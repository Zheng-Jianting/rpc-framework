package com.zhengjianting.dto;

import lombok.*;

import java.io.Serializable;

@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class RpcRequest implements Serializable {
    private static final long serialVersionUID = -6446802601200300663L;

    private String requestId;

    private String methodName;
    private Class<?>[] paramTypes;
    private Object[] parameters;

    private String interfaceName;
    private String group;
    private String version;

    public String getRpcServiceName() {
        return this.getInterfaceName() + this.getGroup() + this.getVersion();
    }
}