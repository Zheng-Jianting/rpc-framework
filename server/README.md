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

