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

- RpcRequestTransportImpl：客户端通过 `Socket` 传输 `RpcRequest` 至服务端，并接收响应。其中，序列化和反序列化使用 `Protobuf`

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
              log.info(address.toString());
              socket.connect(address);
  
              // serializer
              Serializer serializer = new ProtobufSerializerImpl();
  
              // send request to server
              OutputStream out = socket.getOutputStream();
              out.write(serializer.serialize(rpcRequest));
              out.flush();
              socket.shutdownOutput();
  
              // receive response from server
              InputStream in = socket.getInputStream();
              byte[] responseBuff = new byte[0];
              byte[] buff = new byte[1024];
              int k = -1;
              while((k = in.read(buff, 0, buff.length)) > -1) {
                  byte[] tempBuff = new byte[responseBuff.length + k]; // temp buffer size = bytes already read + bytes last read
                  System.arraycopy(responseBuff, 0, tempBuff, 0, responseBuff.length); // copy previous bytes
                  System.arraycopy(buff, 0, tempBuff, responseBuff.length, k);  // copy current lot
                  responseBuff = tempBuff; // call the temp buffer as your result buff
              }
  
              return serializer.deserialize(responseBuff, RpcResponse.class);
          } catch (IOException e) {
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
