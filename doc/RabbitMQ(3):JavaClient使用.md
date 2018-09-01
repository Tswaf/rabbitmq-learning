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

## 连接到RabbitMQ
通过ConnectionFactory工厂方法，设置连接属性，生成Connection对象，建立客户端到RabbitMQ的连接。在Connection上创建Channel，建立一个连接通道。
Channel不是线程安全的，在实际使用中，应该通过Connection为每个线程创建独立的Channel。
```
#设置连接属性。未设置时使用默认值:使用默认账号guest连接到localhost到默认vhost "/"
ConnectionFactory connectionFactory = new ConnectionFactory();
connectionFactory.setHost("localhost");
connectionFactory.setPort(5672);
connectionFactory.setVirtualHost("/");
connectionFactory.setUsername("guest");
connectionFactory.setPassword("guest");

#生成Connection & Channel
Connection connection = connectionFactory.newConnection();
Channel channel = connection.createChannel();
```
也可以通过设置URI的方式来建立连接:
```
ConnectionFactory connectionFactory = new ConnectionFactory();
connectionFactory.setUri("amqp://username:password@hostname:port/vhost");
Connection connection = connectionFactory.newConnection();
Channel channel = connection.createChannel();
```
Channel接口上定义AMQP协议几乎所有的操作。建立好到RabbitQM到连接后，就可以在Channel对象上执行AMQP的操作，如声明队列、交换器、绑定等。

## 创建/删除exchange、queue和binding

### queue操作
#### 声明队列
Channel定义来以下三组方法来声明队列:
1. 普通的queueDeclare方，有两个重载版本

    ```
    Queue.DeclareOk queueDeclare()
    Queue.DeclareOk queueDeclare(String queue, boolean durable, boolean exclusive, boolean autoDelete,Map<String, Object> arguments)
    ```
    第一个不带参数的queueDeclare()方法声明一个队列，队列名称由rabbitMQ自动生成，该队列事非持久的、排他的、自动删除的；  
    
    第二个方法声明队列，可以指定用户设定的队列属性和参数，是最常用的方法。其中各个参数含义如下：
    - queue: 队列名称
    - durable: 队列是否持久话。持久化以为着队列可以从RabbitMQ重启中恢复。
    - exclusive: 排他性。排他性的队列只对首次声明它的连接（Connection而不是Channel）可见，并将在连接断开是自动删除队列。排他性的队列被首次声明后，
    其他连接是不允许创建同名队列的，这种类型的队列使用场景很有限。
    - autoDelete: 队列是否自动删除。自动删除的前提是，至少有一个消费者连接到该队列，而后由断开来连接，队列没有任何消费者时，队列被自动删除。
2. queueDeclareNoWait方法
    ```
    void queueDeclareNoWait(String queue, boolean durable, boolean exclusive, boolean autoDelete, Map<String, Object> arguments)
    ```
    使用queueDeclareNoWait方法声明队列时，不等待服务端到响应，直接返回。这种情况下，声明完队列后立即使用可能引发异常。
3. queueDeclarePassive方法
    ```
    Queue.DeclareOk queueDeclarePassive(String queue)
    ```
    最后一个queueDeclarePassive方法不是真正对声明队列，而只是检查队列是否存在，如果队列存在则正常返回；否则会抛出一个异常，并且执行该操作对Channel不再
    可用，后续应该创建新的Channel对象使用。
#### 删除队列
删除队列有三个方法:
1. 直接删除
    ```
    Queue.DeleteOk queueDelete(String queue) throws IOException;
    ```
    该方法会直接删除掉指定的队列，而不队列对状态，如对是否正在使用、队列中是否还有数据等。
2. 按需删除
    ```
    Queue.DeleteOk queueDelete(String queue, boolean ifUnused, boolean ifEmpty) throws IOException;
    void queueDeleteNoWait(String queue, boolean ifUnused, boolean ifEmpty) throws IOException;
    ```
    指定ifUnused为true，则只有当队列未使用是才会被删除；指定ifEmpty，则只有当队列为空，里面没数据是才会被删除。
3. 清空队列
    queuePurge不删除队列，而是清空队列中数据。
    ```
    Queue.PurgeOk queuePurge(String queue) throws IOException;
    ```
### exchange操作
#### 声明exchange
声明exchange的方法也分为三组:
1. 普通的exchangeDeclare方法

    ```
    Exchange.DeclareOk exchangeDeclare(String exchange, String type) throws IOException;
    Exchange.DeclareOk exchangeDeclare(String exchange, BuiltinExchangeType type) throws IOException;
    Exchange.DeclareOk exchangeDeclare(String exchange, String type, boolean durable) throws IOException;
    Exchange.DeclareOk exchangeDeclare(String exchange, BuiltinExchangeType type, boolean durable) throws IOException;
    Exchange.DeclareOk exchangeDeclare(String exchange, String type, boolean durable, boolean autoDelete, Map<String, Object> arguments) throws IOException;
    Exchange.DeclareOk exchangeDeclare(String exchange, BuiltinExchangeType type, boolean durable, boolean autoDelete, Map<String, Object> arguments) throws IOException;
    Exchange.DeclareOk exchangeDeclare(String exchange,
                                       String type,
                                       boolean durable,
                                       boolean autoDelete,
                                       boolean internal,
                                       Map<String, Object> arguments) throws IOException;                                           
    Exchange.DeclareOk exchangeDeclare(String exchange,
                                       BuiltinExchangeType type,
                                       boolean durable,
                                       boolean autoDelete,
                                       boolean internal,
                                       Map<String, Object> arguments) throws IOException;

    ```
    各个参数含义如下：
    - exchange: 交换器的名称
    - type/BuiltinExchangeType: 交换器的类型
    - durable: 是否持久化。持久化的交换器会从RabbitMQ服务重启中恢复，而不用重新创建。
    - autoDelete: 是否自动删除。自动删除的前提是，只是有一队列或交换器与该交换器绑定，之后所有与该交换器绑定的队列/交换器都进行来解绑。
    - internal: 是否为内置交换器。内置交换器是不允许客户端发送消息的。内置交换使用的场景是与其他交换器绑定（RabbitMQ扩展，非AMQP原生功能）
    - arguments: 其他的结构话参数
     
2. exchangeDeclareNoWait方法
    使用exchangeDeclareNoWait方法声明exchange，方法调用不等待服务端的响应，直接返回，各个参数含义与上面相同。所以声明exchange后立即使用，很可能引发异常。

    ```
    void exchangeDeclareNoWait(String exchange,
                               String type,
                               boolean durable,
                               boolean autoDelete,
                               boolean internal,
                               Map<String, Object> arguments) throws IOException; 
    void exchangeDeclareNoWait(String exchange,
                               BuiltinExchangeType type,
                               boolean durable,
                               boolean autoDelete,
                               boolean internal,
                               Map<String, Object> arguments) throws IOException; 
    ```
    
3. exchangeDeclarePassive方法
    与queueDeclarePassive方法类似，exchangeDeclarePassive用来检查exchange是否存在，而不会创建exchange。
  
    ```
    Exchange.DeclareOk exchangeDeclarePassive(String name) throws IOException;     
    ```
#### 删除exchange
与删除队列类似，可以直接删除交换器或是按需删除。
1. 直接删除
    ```
    Exchange.DeleteOk exchangeDelete(String exchange) throws IOException;
    ```
2. 按需删除
    ```
    Exchange.DeleteOk exchangeDelete(String exchange, boolean ifUnused) throws IOException;
    void exchangeDeleteNoWait(String exchange, boolean ifUnused) throws IOException;
    ```
    ifUnused设置为true是，只有当交换器未被使用是，才会被删除。
### binding操作