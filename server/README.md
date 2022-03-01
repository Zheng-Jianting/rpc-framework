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

  

