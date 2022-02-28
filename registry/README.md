## Registry

### config

- RpcServiceConfig：约定注册于 zookeeper 的服务的格式

  ```java
  Hello hello = new HelloImpl();
  
  // class com.zhengjianting.HelloImpl
  hello.getClass();
  
  // 类调用 getInterface() 返回其实现的接口数组, 接口调用返回其继承的接口数组
  // interface com.zhengjianting.Hello
  hello.getClass().getInterface()[0];
  
  // com.zhengjianting.Hello
  hello.getClass().getInterface()[0].getCanonicalName();
  ```

  

- 待定

### zookeeper

- ServiceRegistry

  ```java
  // 为 RpcServiceConfig.getRpcServiceName() 标识的服务(接口) 创建一个持久化节点
  // 节点路径 eg: rpc/com.zhengjianting.Hello/127.0.0.1:10526
  public interface ServiceRegistry {
      void registerService(String rpcServiceName, InetSocketAddress address);
  }
  
  public class ServiceRegistryImpl implements ServiceRegistry {
      @Override
      public void registerService(String rpcServiceName, InetSocketAddress address) {
          String path = CuratorUtils.ZK_REGISTER_ROOT_PATH + "/" + rpcServiceName + address.toString();
          CuratorFramework zkClient = CuratorUtils.getZkClient();
          CuratorUtils.createPersistentNode(zkClient, path);
      }
  }
  ```

  

- 待定

### provider

`ServiceProvider` 记录已注册的服务（接口），并通过 `ServiceRegistry` 发布一个服务。

```java
// key: RpcServiceConfig.getRpcServiceName()
// value: RpcServiceConfig.getService()
private final Map<String, Object> serviceMap;

// element: RpcServiceConfig.getRpcServiceName()
private final Set<String> registeredService;

// 用于注册一个服务, 作用如上所述
private final ServiceRegistry serviceRegistry;
```

question：

- ```java
  // 是否因为等同于 serviceMap.keySet() 而可以省略
  private final Set<String> registeredService;
  ```

- ```java
  // 在使用 ServiceRegistry 注册时没有判断是否重复, 重复注册是否会出错
  void publishService(RpcServiceConfig rpcServiceConfig);
  ```







