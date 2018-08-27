# RabbitMQ: Java Client使用
RabbitMQ针对不同的开发语言（java，python，c/++，Go等等），提供了丰富对客户端，方便使用。就Java而言，可供使用的客户端有RabbitMQ Java client、
RabbitMQ JMS client、apache的camel-rabbitmq、以及Banyan等。在Spring中，也可以使用Spring AMQP、Spring Cloud Data Flow方便对集成RabbitMQ。  
实际开发使用中，RabbitMQ Java client和Spring AMQP比较常用。RabbitMQ Java client在使用上更加接近AMQP协议，Spring AMQP则更加方便Spring项目中的集成。本为总结RabbitMQ Java client
的使用。

## Java客户端概览
RabbitMQ的Java客户端包为*amqp-client-{version}.jar*,可以从RabbitMQ官网下载后引入项目中。
 
对于Maven工程，pom.xml中加入以下依赖即可引入RabbitMQ的Java客户端：
```
<dependency>
    <groupId>com.rabbitmq</groupId>
    <artifactId>amqp-client</artifactId>
    <version>5.2.0</version>
</dependency>
```
amqp-client客户端主要包含以下包：

| 包 | 说明 |  
| :---- | :---- |  
| com.rabbitmq.client| 客户端api定义，提供了接口和类定义AMQP中connection,channel,queue,exchange等核心对象 |  
| com.rabbitmq.client.impl | 客户端具体实现 |  
| com.rabbitmq.client.impl.nio | 客户端nio实现 |  
| com.rabbitmq.client.impl.recovery |  |  
| com.rabbitmq.tool.json | 对象json工具类 |  
| com.rabbitmq.tool.jsonrpc | 基于AMQP等json-rpc支持 |  
| com.rabbitmq.util | 客户端中使用等工具累 |  

对普通用户而言，一般只需关注com.rabbitmq.client包，其中定了AMQP协议中对基础对象，包含以下主要接口和类：
- Channel: AMQP 0-9-1 Channel对象，表示一个连接通道，提供了大多数AMQP操作，如创建队列、交换器、绑定等
- Connection: AMQP 0-9-1 Connection，表示一个客户端连接
- ConnectionFactory: Connectiong工厂
- Consumer: 消息消费者接口
- DefaultConsumer: 消费者接口默认实现
- BasicProperties: 消息属性对象，用于发送消息时设置消息属性
- BasicProperties.Builder: BasicProperties构建器

使用ConnectionFactory创建出Connection对象，在使用Connection对象创建一个Channel，在Channel上即可完成基本的发送消息，消费消息等AMQP操作；
发送消息时，可通过BasicProperties设置消息属性；可以通过实现Consumer接口或继承DefaultConsumer类实现一个消费者来消费消息。总之，通过以上对象，
即可完成基本的消息从生产到消费的全流程。