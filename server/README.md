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

  在 `run()` 方法中，首先需要将客户端传输的数据反序列化为 `RpcRequest`，再调用 `RpcRequestHandler` 对象的方法执行目标函数（服务），获得结果后将其序列化为 `RpcResponse` 并传输至客户端。其中，序列化和反序列化使用 `Protobuf`

  在使用 `NIO` 后，数据都需要通过缓冲区 `ByetBuffer` 对 `SocketChannel` 进行读写。

### NIO

在使用传统的 BIO 通信方式时，假设服务端同时接收到 1000 个连接请求，服务端为每一个请求新建一个线程去处理，服务端线程继续监听客户端请求。这样实现的缺点很明显，如果一个请求建立连接后，处理这个请求的线程如果迟迟接收不到报文，这个线程就会一直阻塞直至报文到达服务端。创建线程的代价是十分大的，在高并发的情况下是不可取的，即使使用线程池，也会导致阻塞队列积压的任务过多。

多路复用可以使得一个线程拥有监听多个 IO 事件的能力，只有当选择器轮询到某个通道有 IO 事件准备好时，才会新建一个线程去处理。

因此，通道必须被配置为非阻塞的。如果通道被配置为阻塞的，那么选择器轮询到这个通道时，恰好这个通道还没有 IO 事件准备好，那么选择器就会阻塞在这个通道，不能轮询其它通道。这就相当于饭店的服务员等到上菜后才去服务下一位客人，而事实上，当一位客人点完菜后就可以去服务下一位客人。这就相当于 BIO 通信方式，并且服务端为单线程的情况。

### 线程池

使用线程池进行优化后，当 `RpcNIOServer` 的选择器监听到某个通道有 IO 事件准备好时，通过线程池新建一个线程去处理这个 IO 事件。因此，主线程可以继续通过选择器轮询是否有通道准备好进行 IO，而不用阻塞在主线程，等待当前这个通道的 IO 事件处理完毕。

这里也碰到了一个问题，因为在连接建立后，服务端为这次连接新建了一个 `SocketChannel` 进行数据传输，并且将这个通道注册到选择器上以便监听该通道的 IO 事件。但是，因为数据并不是全部封装到一个报文段而只传输一次，比如当数据比较多时，客户端发送的数据会进行分组，封装为多个 TCP Segment 进行传输，这也就意味着服务端的选择器进行轮询时，会监听到多次 `SocketChannel` 的 IO 事件。因此我们在监听到 `SocketChannel` 的第一次 IO 事件时，可以为其新建一个线程去处理，从而不阻塞主线程，但是，对于这个 `SocketChannel` 之后到达的 IO 事件，我们不能再新建一个线程，因为已经有线程在处理了。而且即使再次新建了线程，读取的数据也是不完整的，无法反序列化为 `RpcRequest`。

于是，在为 `SocketChannel` 到达的第一次 IO 事件创建线程后，应当让选择器不再监听这个 `SocketChannel`。

```java
selectionKey.cancel();
```



