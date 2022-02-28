## client

### dto

- RpcRequest：数据传输对象

  其中 `getRpcServiceName()` 和 `com.zhengjianting.config.RpcServiceConfig` 一致，因为后续需要根据这个值查找实现了该服务的服务端地址（zookeeper 节点路径 eg: `rpc/com.zhengjianting.Hello/127.0.0.1:10526`）

  ```java
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
  ```

### transport

- RpcRequestTransportImpl：客户端通过 `Socket` 传输 `RpcRequest` 至服务端，并接收响应。

  ```java
  @Slf4j
  public class RpcRequestTransportImpl implements RpcRequestTransport {
      private final ServiceDiscovery serviceDiscovery;
  
      public RpcRequestTransportImpl() {
          this.serviceDiscovery = ExtensionLoader.getExtensionLoader(ServiceDiscovery.class).getExtension("zookeeper");
      }
  
      @Override
      public Object sendRpcRequest(RpcRequest rpcRequest) {
          InetSocketAddress address = serviceDiscovery.lookupService(rpcRequest);
          try {
              Socket socket = new Socket();
              socket.connect(address);
  
              // send request to server
              ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
              out.writeObject(rpcRequest);
  
              // receive response from server
              ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
              return in.readObject();
          } catch (IOException | ClassNotFoundException e) {
              log.error(e.getMessage(), e);
              throw new RuntimeException("error from sendRpcRequest.");
          }
      }
  }
  ```

  其中，`ServiceDiscovery` 调用 `lookupService(rpcRequest)` 查找实现了该服务的服务端地址。

### proxy

- RpcClientProxy：动态代理，通过 `getProxy(Class<T> clazz)` 获得代理对象，运行时执行的是 `invoke(Object proxy, Method method, Object[] args)` 方法。

  ```java
  // eg: 
  interface HelloService() {
      String hello(String name);
      String goodbye(String name);
  }
  
  // 代理对象, 类似于创建了一个 HelloService 的匿名实现类
  HelloService helloService = Proxy.newProxyInstance(HelloService.class.getClassLoader(), new Class<?>[] { HelloService.class }, handler);
  
  // 当调用代理对象的方法时, 实际上执行的是 handler.invoke(Object proxy, Method method, Object[] args)
  helloService.hello("zjt");
  helloService.goodbye("zjt");
  
  // 创建 handler (也可以 implements InvocationHandler 并重写 invoke 方法)
  InvocationHandler handler = new InvocationHandler() {
      // 重写 invoke 方法
      @Override
      public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
          // 通过反射, 可以在运行时确定代理对象执行的是什么方法, 参数是什么
          if (method.getName().equals("hello")) {
              System.out.println("hello, " + args[0]);
          }
          else if (method.getName().equals("goodbye")) {
              System.out.println("goodbye, " + args[0]);
          }
      }
  }
  ```

  