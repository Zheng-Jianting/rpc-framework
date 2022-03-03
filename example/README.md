## example

### resources（rpc.properties）

- client

  在 `RpcRequestTransportImpl` 中，发送 `RpcRequest` 之前需要查找实现了该接口（服务）的服务端地址：

  ```java
  private final ServiceDiscovery serviceDiscovery;
  InetSocketAddress address = serviceDiscovery.lookupService(rpcRequest);
  ```

  在 `ServiceDiscovery` 的实现类 `ServiceDiscoveryImpl` 中，需要获取调用 `CuratorUtils.getZkClient()` 方法获取 zookeeper client，在该方法中需要读取配置文件：

  ```properties
  rpc.zookeeper.address = 127.0.0.1:2181
  ```

  ```java
  public class ServiceDiscoveryImpl implements ServiceDiscovery {
      @Override
      public InetSocketAddress lookupService(RpcRequest rpcRequest) {
          String rpcServiceName = rpcRequest.getRpcServiceName();
          CuratorFramework zkClient = CuratorUtils.getZkClient();
          List<String> addresses = CuratorUtils.getChildrenNodes(zkClient, rpcServiceName);
          if (addresses == null || addresses.size() == 0) {
              throw new RuntimeException("No server implementing this service was found.");
          }
  
          // default select the first address
          String host = addresses.get(0).split(":")[0];
          int port = Integer.parseInt(addresses.get(0).split(":")[1]);
  
          return new InetSocketAddress(host, port);
      }
  }
  ```

- server

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

