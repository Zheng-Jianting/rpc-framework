## server

### factory

- SingleFactory：单例工厂

### handler

- RpcRequestHandler

  根据客户端传输的 RpcRequest 对象，查找实现了该服务的对象：

  ```java
  Object service = serviceProvider.getService(rpcRequest.getRpcServiceName());
  ```

  再结合反射调用这个对象实现的对应方法：

  ```java
  Method method = service.getClass().getMethod(rpcRequest.getMethodName(), rpcRequest.getParamTypes());
  result = method.invoke(service, rpcRequest.getParameters());
  ```

- RpcRequestHandlerRunnable

  当 `RpcNIOServer` 的轮询代理器（选择器）`selector` 监听到有 `IO` 事件到达时，会使用线程池创建一个线程处理这个 `IO` 事件（此处指客户端 `socket` 经三次握手和 `RpcNIOServer` 中的 `serverSocket` 建立连接后，客户端发送的 `RpcRequest` 经网络传输到达服务端操作系统内核）。

  在 `run()` 方法中会调用 `RpcRequestHandler` 对象的方法执行目标函数（服务），获得结果后将其封装成 `RpcResponse` 并传输至客户端。

  在 `run()` 方法中没有使用通道 `SocketChannel` 的缓冲区进行序列化和反序列化，使用了 `socket` 的输入输出流简化了操作。

  ```java
  @Slf4j
  public class RpcRequestHandlerRunnable implements Runnable {
      private final SocketChannel socketChannel;
      private final RpcRequestHandler rpcRequestHandler;
  
      public RpcRequestHandlerRunnable(SocketChannel socketChannel) {
          this.socketChannel = socketChannel;
          this.rpcRequestHandler = SingletonFactory.getInstance(RpcRequestHandler.class);
      }
  
      @Override
      public void run() {
          try {
              ObjectInputStream in = new ObjectInputStream(socketChannel.socket().getInputStream());
              ObjectOutputStream out = new ObjectOutputStream(socketChannel.socket().getOutputStream());
              // read rpcRequest
              RpcRequest rpcRequest = (RpcRequest) in.readObject();
              // invoke target method
              Object invokeResult = rpcRequestHandler.handler(rpcRequest);
              // write response
              out.writeObject(RpcResponse.success(invokeResult, rpcRequest.getRequestId()));
              out.flush();
          } catch (IOException | ClassNotFoundException e) {
              log.error(e.getMessage(), e);
              throw new RuntimeException("error from RpcRequestHandlerRunnable.");
          }
      }
  }
  ```


### resources

- rpc.properties

  ```properties
  rpc.zookeeper.address = 127.0.0.1:2181
  ```

  在 `RpcNIOServer` 中，启动服务端之前需要注册服务：

  ```java
  private final ServiceProvider serviceProvider;
  public void registerService(RpcServiceConfig rpcServiceConfig) {
      serviceProvider.publishService(rpcServiceConfig);
  }
  ```

  在 `ServiceProviderImpl` 中，通过 `ServiceRegister` 注册服务：

  ```java
  private final ServiceRegistry serviceRegistry;
  
  @Override
  public void publishService(RpcServiceConfig rpcServiceConfig) {
      try {
          this.addService(rpcServiceConfig);
          String host = InetAddress.getLocalHost().getHostAddress();
          serviceRegistry.registerService(rpcServiceConfig.getRpcServiceName(), new InetSocketAddress(host, 10526));
      } catch (UnknownHostException e) {
          log.error(e.getMessage(), e);
      }
  }
  ```

  在 `ServiceRegisterImpl` 中，需要获取 zookeeper client，根据 rpcServiceName 创建一个持久化节点：

  ```java
  public class ServiceRegistryImpl implements ServiceRegistry {
      @Override
      public void registerService(String rpcServiceName, InetSocketAddress address) {
          String path = CuratorUtils.ZK_REGISTER_ROOT_PATH + "/" + rpcServiceName + address.toString();
          CuratorFramework zkClient = CuratorUtils.getZkClient();
          CuratorUtils.createPersistentNode(zkClient, path);
      }
  }
  ```

  通过 `CuratorUtils.getZkClient()` 获取 zkClient 需要读取配置文件
