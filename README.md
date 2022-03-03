# rpc-framework

### 介绍

这是一个使用 **Java NIO + ZooKeeper + ProtoBuf** 实现的简易 RPC 框架

- Java NIO：服务端基于 Java NIO 实现多路复用的 I/O 模型，并使用线程池进行优化
- ZooKeeper：提供服务的注册和查找功能
- ProtoBuf：进行网络传输时，使用 ProtoBuf 进行序列化和反序列化

### 模块

这个 RPC 框架按功能分为以下模块：

- registry

  - config

    在 config 包下的 RpcServiceConfig 定义了一个 RPC 服务的格式

  - zookeeper

    ServiceRegistry 定义了基本的服务注册接口

    ServiceDiscovery 定义了基本的服务发现接口

    注册服务时会创建一个持久化节点路径类似于：`rpc/com.zhengjianting.service.HelloService/127.0.0.1:10526`，因此查找实现 RPC 服务的服务端地址时，只需要查找服务名为根的子树的子节点即可，这里没有实现负载均衡，默认调用的是该子树的第一个子节点标识的服务端地址。

  - provider

    对 ServiceRegistry 进行封装，记录了服务端已注册的服务，以及该服务对应的的实现类。使得服务端接收到 RpcRequest 时，可以根据请求的服务，并结合反射调用该服务的实现类对应的方法。

- client

  - proxy

    RpcClientProxy 是客户端的代理对象，当客户端需要调用某个接口的方法时，首先通过 getProxy 方法得到这个接口的代理对象（和接口同类型），再通过这个代理对象调用即可。

    即动态代理，客户端无需编写接口的实现类，当调用代理对象的任何方法时，运行时实际执行的都是 InvocationHandler.invoke(...) 方法，在该方法中将调用的信息封装为 RpcRequest，序列化后通过 socket 传输至服务端，并返回服务端的处理结果。

  - transport

    RpcRequestTransportImpl 序列化 RpcRequest，通过 socket 进行网络传输，反序列化 RpcResponse.

- server

  - handler

    RpcRequestHandler 根据请求调用对应的方法，并返回结果

    RpcRequestHandlerRunnable：处理一次 RPC 请求的线程（当 I/O 事件准备好时才会被创建出来，即位于 RpcNIOServer 的选择器轮询到 selectionKey.isReadable），首先通过缓冲区读取 SocketChannel 传输的数据，将其反序列化为 RpcRequest，调用对应的方法之后，将结果封装为 RpcResponse，经序列化后写入 SocketChannel。

  - transport

    RpcNIOServer 使用 Java NIO 实现基于多路复用 I/O 模型的服务端，通过将非阻塞通道 Channel 注册到选择器 Selector，选择器不断轮询的方式，使得一个线程拥有监听多个 I/O 事件的能力。

    RpcNIOServer 还使用线程池进行优化，当轮询到某个通道准备好进行 I/O 时，新建一个线程去处理，从而主线程不必阻塞在此等待该通道 I/O 完成，得以继续轮询并处理其它通道 I/O 事件。

- serialization

  实现 ProtoBuf 序列化以及反序列化

- service-api

  服务接口，客户端获取对应的代理对象后直接调用方法即可，无需编写接口的实现类

- example

  一个简单的客户端，服务端示例，服务端需要实现相应的服务接口，启动服务端之前需要先将服务接口注册于 ZooKeeper.