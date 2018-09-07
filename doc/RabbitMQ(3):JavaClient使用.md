# RabbitMQ: Java Client使用
RabbitMQ针对不同的开发语言（java，python，c/++，Go等等），提供了丰富对客户端，方便使用。就Java而言，可供使用的客户端有RabbitMQ Java client、
RabbitMQ JMS client、apache的camel-rabbitmq、以及Banyan等。在Spring中，也可以使用Spring AMQP、Spring Cloud Data Flow方便对集成RabbitMQ。  
实际开发使用中，RabbitMQ Java client和Spring AMQP比较常用。RabbitMQ Java client在使用上更加接近AMQP协议，Spring AMQP则更加方便Spring项目中的集成。本为总结RabbitMQ Java client
的使用。

## Java客户端概览
RabbitMQ的Java客户端包为*amqp-client-{version}.jar*,可以从RabbitMQ官网下载后引入项目中。
 
对于Maven工程，pom.xml中加入以下依赖即可引入RabbitMQ的Java客户端：
```xml
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
```java
//设置连接属性。未设置时使用默认值:使用默认账号guest连接到localhost到默认vhost "/"
ConnectionFactory connectionFactory = new ConnectionFactory();
connectionFactory.setHost("localhost");
connectionFactory.setPort(5672);
connectionFactory.setVirtualHost("/");
connectionFactory.setUsername("guest");
connectionFactory.setPassword("guest");

//生成Connection & Channel
Connection connection = connectionFactory.newConnection();
Channel channel = connection.createChannel();
```
也可以通过设置URI的方式来建立连接:

```java
ConnectionFactory connectionFactory = new ConnectionFactory();
connectionFactory.setUri("amqp://username:password@hostname:port/vhost");
Connection connection = connectionFactory.newConnection();
Channel channel = connection.createChannel();
```
Channel接口上定义AMQP协议几乎所有的操作。建立好到RabbitMQ到连接后，就可以在Channel对象上执行AMQP的操作，如声明队列、交换器、绑定等。

## 操作exchange、queue和binding

### queue操作
#### 声明队列
Channel定义来以下三组方法来声明队列:
1. 普通的queueDeclare方，有两个重载版本

    ```java
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

    ```java
    void queueDeclareNoWait(String queue, boolean durable, boolean exclusive, boolean autoDelete, Map<String, Object> arguments)
    ```
    使用queueDeclareNoWait方法声明队列时，不等待服务端到响应，直接返回。这种情况下，声明完队列后立即使用可能引发异常。
3. queueDeclarePassive方法

    ```java
    Queue.DeclareOk queueDeclarePassive(String queue)
    ```
    最后一个queueDeclarePassive方法不是真正对声明队列，而只是检查队列是否存在，如果队列存在则正常返回；否则会抛出一个异常，并且执行该操作对Channel不再
    可用，后续应该创建新的Channel对象使用。
    
#### 删除队列
删除队列有三个方法:
1. 直接删除

    ```java
    Queue.DeleteOk queueDelete(String queue) throws IOException;
    ```
    该方法会直接删除掉指定的队列，而不队列对状态，如对是否正在使用、队列中是否还有数据等。
2. 按需删除

    ```java
    Queue.DeleteOk queueDelete(String queue, boolean ifUnused, boolean ifEmpty) throws IOException;
    void queueDeleteNoWait(String queue, boolean ifUnused, boolean ifEmpty) throws IOException;
    ```
    指定ifUnused为true，则只有当队列未使用是才会被删除；指定ifEmpty，则只有当队列为空，里面没数据是才会被删除。
3. 清空队列  
    queuePurge不删除队列，而是清空队列中数据。
    
    ```java
    Queue.PurgeOk queuePurge(String queue) throws IOException;
    ```

### exchange操作
#### 声明exchange
声明exchange的方法也分为三组:
1. 普通的exchangeDeclare方法

    ```java
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
    - arguments: 其他的结构化参数
     
2. exchangeDeclareNoWait方法
    使用exchangeDeclareNoWait方法声明exchange，方法调用不等待服务端的响应，直接返回，各个参数含义与上面相同。所以声明exchange后立即使用，很可能引发异常。

    ```java
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
  
    ```java
    Exchange.DeclareOk exchangeDeclarePassive(String name) throws IOException;     
    ```
#### 删除exchange
与删除队列类似，可以直接删除交换器或是按需删除。
1. 直接删除

    ```java
    Exchange.DeleteOk exchangeDelete(String exchange) throws IOException;
    ```
2. 按需删除

    ```java
    Exchange.DeleteOk exchangeDelete(String exchange, boolean ifUnused) throws IOException;
    void exchangeDeleteNoWait(String exchange, boolean ifUnused) throws IOException;
    ```
    ifUnused设置为true时，只有当交换器未被使用时，才会被删除。
    
### binding操作
#### 队列与交换器绑定/解绑
把队列和交换器绑定起来，使用queueBind方法：

```java
Queue.BindOk queueBind(String queue, String exchange, String routingKey) throws IOException;
Queue.BindOk queueBind(String queue, String exchange, String routingKey, Map<String, Object> arguments) throws IOException;

void queueBindNoWait(String queue, String exchange, String routingKey, Map<String, Object> arguments) throws IOException;
```
被绑定的队列和交换器的名称分别由参数queue和exchange指定，绑定的key通过参数routingKey指定。arguments指定了其他的结构化参数。  
对应于绑定方法，有如下两个解绑方法：

```java
Queue.UnbindOk queueUnbind(String queue, String exchange, String routingKey) throws IOException;
Queue.UnbindOk queueUnbind(String queue, String exchange, String routingKey, Map<String, Object> arguments) throws IOException;
```
#### 交换器与交换器绑定/解绑
作为RabbitMQ对AMQP的扩展功能，交换器允许和别的交换器绑定起来。这和队列与交换器对绑定在使用上没有太大对不同。可以将交换器E1和E2绑定起来，在将E2和队列
Q绑定起来，之后生产者向E1发送消息，消息在E1上被路由到E2，再由E2路由到Q。  
绑定交换器和其他交换器使用如下方法：

```java
Exchange.BindOk exchangeBind(String destination, String source, String routingKey) throws IOException;
Exchange.BindOk exchangeBind(String destination, String source, String routingKey, Map<String, Object> arguments) throws IOException;

void exchangeBindNoWait(String destination, String source, String routingKey, Map<String, Object> arguments) throws IOException;
```
destination和source参数分别指定目的交换器和源交换器。  
同样，通过以下方法解绑：

```java
Exchange.UnbindOk exchangeUnbind(String destination, String source, String routingKey) throws IOException;
Exchange.UnbindOk exchangeUnbind(String destination, String source, String routingKey, Map<String, Object> arguments) throws IOException;

void exchangeUnbindNoWait(String destination, String source, String routingKey, Map<String, Object> arguments) throws IOException;
```

何时需要创建队列、交换器以及它们之间的绑定关系呢？这取决与应用程序。在一些复杂应用中，可能需要管理员事先规划好交换器、队列以及绑定关系，并在服务队创建好
这些资源，客户端直接去使用即可，这样便于统一规划，防止客户端随意创建资源造成错误或是资源浪费；在其他一些应用中，可能由客户端自己创建这些资源并使用。总之，
这取决于应用本身的规划。


## 发送消息
在channel对象上，使用basicPublish方法，将消息发送到指定的交换器。basicPublish有三个重载方法：

```java
void basicPublish(String exchange, String routingKey, BasicProperties props, byte[] body) throws IOException;
void basicPublish(String exchange, String routingKey, boolean mandatory, BasicProperties props, byte[] body) throws IOException;
void basicPublish(String exchange, String routingKey, boolean mandatory, boolean immediate, BasicProperties props, byte[] body) throws IOException;
```
发送消息是，指定的各个参数含义如下：
- exchange: exchange名称，指定消息发送到哪个交换器
- routingKey: 消息到路由键
- mandatory: 该参数决定消息到达exchange后无法路由时到处理方式。指定为true，当消息到达exchange时，如果根据路由键找不到匹配到队列，该消息将被返回给
客户端；否则，无法路由到消息被直接丢弃。
- immediate: 该参数设置为true，消息在从exchange路由到队列时，如果队列上消费者，消息将被投递到队列，否则将消息返回给生产者。不过，该参数从RabbitMQ 3.0版本开始已经废弃掉。
- props: 指定消息到其他属性,如header、deliveryMode、contentType等。
- body: 消息体

## 消费消息
在RabbitMQ中，消费者可以以推模式和拉模式两种方式消费消息，分别对象basicConsume方法和basicGet方法。推模式中，由服务端主动推送消息到消费这，消费者在
会调方法中处理消息，然后响应服务端；拉模式中，消费者每次主动从队列中拉起单条消息消费，然后响应服务端。
### 推模式
推模式使用basicConsume方法来消费消息，可以看到，RabbitMQ定义了basicConsume方法许多重载版本：

```java
String basicConsume(String queue, Consumer callback) throws IOException;
String basicConsume(String queue, DeliverCallback deliverCallback, CancelCallback cancelCallback) throws IOException;
String basicConsume(String queue, DeliverCallback deliverCallback, ConsumerShutdownSignalCallback shutdownSignalCallback) throws IOException;
String basicConsume(String queue, DeliverCallback deliverCallback, CancelCallback cancelCallback, ConsumerShutdownSignalCallback shutdownSignalCallback) throws IOException;
String basicConsume(String queue, boolean autoAck, Consumer callback) throws IOException;
String basicConsume(String queue, boolean autoAck, DeliverCallback deliverCallback, CancelCallback cancelCallback) throws IOException;
String basicConsume(String queue, boolean autoAck, DeliverCallback deliverCallback, ConsumerShutdownSignalCallback shutdownSignalCallback) throws IOException;
String basicConsume(String queue, boolean autoAck, DeliverCallback deliverCallback, CancelCallback cancelCallback, ConsumerShutdownSignalCallback shutdownSignalCallback) throws IOException;
String basicConsume(String queue, boolean autoAck, Map<String, Object> arguments, Consumer callback) throws IOException;
String basicConsume(String queue, boolean autoAck, Map<String, Object> arguments, DeliverCallback deliverCallback, CancelCallback cancelCallback) throws IOException;
String basicConsume(String queue, boolean autoAck, Map<String, Object> arguments, DeliverCallback deliverCallback, ConsumerShutdownSignalCallback shutdownSignalCallback) throws IOException;
String basicConsume(String queue, boolean autoAck, Map<String, Object> arguments, DeliverCallback deliverCallback, CancelCallback cancelCallback, ConsumerShutdownSignalCallback shutdownSignalCallback) throws IOException;
String basicConsume(String queue, boolean autoAck, String consumerTag, Consumer callback) throws IOException;
String basicConsume(String queue, boolean autoAck, String consumerTag, DeliverCallback deliverCallback, CancelCallback cancelCallback) throws IOException;
String basicConsume(String queue, boolean autoAck, String consumerTag, DeliverCallback deliverCallback, ConsumerShutdownSignalCallback shutdownSignalCallback) throws IOException;
String basicConsume(String queue, boolean autoAck, String consumerTag, DeliverCallback deliverCallback, CancelCallback cancelCallback, ConsumerShutdownSignalCallback shutdownSignalCallback) throws IOException;
String basicConsume(String queue, boolean autoAck, String consumerTag, boolean noLocal, boolean exclusive, Map<String, Object> arguments, Consumer callback) throws IOException;
String basicConsume(String queue, boolean autoAck, String consumerTag, boolean noLocal, boolean exclusive, Map<String, Object> arguments, DeliverCallback deliverCallback, CancelCallback cancelCallback) throws IOException;
String basicConsume(String queue, boolean autoAck, String consumerTag, boolean noLocal, boolean exclusive, Map<String, Object> arguments, DeliverCallback deliverCallback, ConsumerShutdownSignalCallback shutdownSignalCallback) throws IOException;
String basicConsume(String queue, boolean autoAck, String consumerTag, boolean noLocal, boolean exclusive, Map<String, Object> arguments, DeliverCallback deliverCallback, CancelCallback cancelCallback, ConsumerShutdownSignalCallback shutdownSignalCallback) throws IOException;
```
各个参数含义如下：
- queue: 指定队列名称，即消费哪个队列里的数据。
- autoAck: 是否自动进行消息确认。设置为true表示自动消息确认，消费者收到消息时，自动向服务端发送ack信号，服务端收到该信号，则认为消息已经被消费，将其从队列中删除。
设置为false值，需要消费者手动进行ack。
- consumerTag: 消费者标签，用了区分多个消费者。
- noLocal: 设置为true，表示不能将同一个Connection中生成者发送的消息传递给这个Connection中的消费者。
- exclusive: 是否排他。
- arguments: 其他结构化参数。
- callback: 消费者会调函数，处理发送过来的消息。
- cancelCallback: 消费者取消订阅时的回调方法。
- shutdownSignalCallback: channel或者connection关闭时的回调方法。
- 其他回调方法...

可见，消费者主要还是定义各个回调方法，然后调用basicConsume方法消费消息。最主要的回调参数还是callback，定了了消费者消费消息的主要流程，可以同实现
Consumer接口或继承DefaultConsumer类来实现一个Consumer，如:

```java
String QUEUE_NAME ="";
boolean autoAck = false;
channel.basicConsume(QUEUE_NAME,autoAck,"myTag",
                new DefaultConsumer(channel){
                    @Override
                    public void handleDelivery(String consumerTag,
                                               Envelope envelope,
                                               AMQP.BasicProperties properties,
                                               byte[] body) throws IOException{
                       long deliveryTag = envelope.getDeliveryTag();
                       process(body,consumerTag,envelope,properties) //消费消息
                       channel.basicAck(deliveryTag,false);
                    }
                });
```
### 拉模式
拉模式只有basicGet一个方法：

```java
GetResponse basicGet(String queue, boolean autoAck) throws IOException;
```
参数queue指定从哪个队列中读取消息；参数autoAck指定是否自动进行消息确认，设定为fasle时，需要消费者对消息消费后，主动向服务端发送ack消息。

```java
GetResponse response = channel.basicGet(QUEUE_NAME, false);
handle(response) //消费消息
channel.basicAck(response.getEnvelope.getDeliveryTag,false);
```
### 拒绝消息
消费者可以拒绝消费投递过来的消息，使用basicReject拒绝单条消息，使用basicNack可以具体多条消息。basicNack不是AMQP的标准操作，而是RabbitMQ的扩展。

```java
void basicReject(long deliveryTag, boolean requeue) throws IOException;
void basicNack(long deliveryTag, boolean multiple, boolean requeue) throws IOException;
```
各个参数含义：
- deliveryTag: 消息的编号
- requeue: 消费者拒绝后，服务端是否将消息重新入队
- multiple: basicNack，multiple参数设置为true时，表明一次性拒绝多条消息：deliveryTag编号之前的所有未被消费的消息都被拒绝。

## 一个完整的示例
我们将实现一个简单的告警处理系统。告警分为io告警和file告警两种类型，告警级别有error和warning两种。  
整体结构如下图所示

![](img/rabbit-example.png)

生产者将告警发送到topic类型的交换器中，交换器更具告警的级别将告警路由分别路由到ErrorQueue和WarningQueue中。消费者ErrorConsumer订阅ErrorQueue
消费error级别的告警消息；IOWarningConsumer订阅WarningQueue,处理warning级别到告警，正如其名称，IOWarningConsumer只对io类型到告警消费，其他类型
的告警reject掉。

实现如下：
- Producer: 随机生产各自类型、级别的告警，并发送到交换器
    
    ```java
    public class Producer {
        public static String EXCHANEG = "alert_exchange";
        public static String WARNING_QUEUE = "waring_queue";
        public static String ERROR_QUEUE = "error_queue";
    
        public static void main(String[] args) throws IOException, TimeoutException {
            //get a default connection and create a channel
            ConnectionFactory cf = new ConnectionFactory();
            Connection connection = cf.newConnection();
            final Channel channel = connection.createChannel();
    
            //declare exchange,queues and bindings
            channel.exchangeDeclare(EXCHANEG, BuiltinExchangeType.TOPIC);
            channel.queueDeclare(WARNING_QUEUE,true,false,false,null);
            channel.queueDeclare(ERROR_QUEUE,true,false,false,null);
            channel.queueBind(WARNING_QUEUE, EXCHANEG,"*.warning");
            channel.queueBind(ERROR_QUEUE, EXCHANEG,"*.error");
    
            while (true){
                String key = randomRoutingKey();
                String message = message(key);
                channel.basicPublish(EXCHANEG,key,null,message.getBytes());
                System.out.println("Sent > " + message);
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    
        public static String[] keys = new String[]{"io.error","io.warning","file.error","file.warning"};
    
        public static String randomRoutingKey(){
            Random random = new Random();
            int rand = Math.abs(random.nextInt())%(keys.length);
            return keys[rand];
        }
    
        public static String message(String key){
            String message = "Alert ..." + " level:" + key;
            return message;
        }
    }
    ```
- ErrorConsumer: 消费error级别到告警
   
    ```java 
    public class ErrorConsumer {
        public static void main(String[] args) throws IOException, TimeoutException {
            ConnectionFactory cf = new ConnectionFactory();
            Connection connection = cf.newConnection();
            final Channel channel = connection.createChannel();
    
            boolean autoAck = false;
            channel.basicConsume(Producer.ERROR_QUEUE,false,new DefaultConsumer(channel){
                @Override
                public void handleDelivery(String consumerTag,
                                           Envelope envelope,
                                           AMQP.BasicProperties properties,
                                           byte[] body) throws IOException{
                    String message = new String(body);
                    //consume message
                    System.out.println("consumed. " + message);
                    channel.basicAck(envelope.getDeliveryTag(),false);
                }
            });
        }
    }
    ```
- IOWarningConsumer: 消费io类型、warning级别的告警。
    ```java
     public class IOWarningConsumer {
        public static void main(String[] args) throws IOException, TimeoutException {
            ConnectionFactory cf = new ConnectionFactory();
            Connection connection = cf.newConnection();
            final Channel channel = connection.createChannel();
    
            boolean autoAck = false;
            channel.basicConsume(Producer.WARNING_QUEUE,autoAck,new DefaultConsumer(channel){
                @Override
                public void handleDelivery(String consumerTag,
                                           Envelope envelope,
                                           AMQP.BasicProperties properties,
                                           byte[] body) throws IOException{
                    String key = envelope.getRoutingKey();
                    String message = new String(body);
                    long deliveryTag = envelope.getDeliveryTag();
                    if (key.startsWith("io")) { // only consumer io warning messages
                        //consume message
                        System.out.println("consumed: " + message);
                        channel.basicAck(deliveryTag,false);
    
                    } else { //reject other messages and requeue them
                        System.out.println("rejected: " + message);
                        channel.basicReject(deliveryTag,true);
                    }
                }
            });
    
        }
    }
    ```

分别启动Producer、ErrorConsumer和IOWarningConsumer。 从输出结果中可以看到: 
- Producer每隔一秒钟随机生产一条消息:
    ```
    Sent > Alert ... level:io.error
    Sent > Alert ... level:file.warning
    Sent > Alert ... level:file.error
    Sent > Alert ... level:io.warning
    Sent > Alert ... level:file.warning
    Sent > Alert ... level:io.error
    Sent > Alert ... level:file.error
    Sent > Alert ... level:file.error
    Sent > Alert ... level:io.warning
    Sent > Alert ... level:file.warning
    Sent > Alert ... level:file.error
    ```
- ErrorConsumer处理了所有error级别的消息:
    ```
    consumed. Alert ... level:io.error
    consumed. Alert ... level:file.error
    consumed. Alert ... level:io.error
    consumed. Alert ... level:file.error
    consumed. Alert ... level:file.error
    consumed. Alert ... level:file.error
    ```
- IOWarningConsumer消费了io类型的warning消息，拒绝了其他消息。
    ```
    rejected: Alert ... level:file.warning
    consumed: Alert ... level:io.warning
    rejected: Alert ... level:file.warning
    consumed: Alert ... level:io.warning
    rejected: Alert ... level:file.warning
    rejected: Alert... level:file.warning
    ...
    ```
    在IOWrningConsumer，对于reject的消息，其requeue参数设置为true，这样消息被拒绝后，在服务端将重新入队，然后又推送给该消费者，一直循环。实际
    应用中，应该避免这种情况到发生，此处只是为了演示reject的用法。
    查看队列状态，可以发现warning_queue中始终存在未被确认的消息：
    ```
    $ rabbitmqctl list_queues name,durable,messages_ready,messages_unacknowledged,messages
    Timeout: 60.0 seconds ...
    Listing queues for vhost / ...
    error_queue true	0	0	0
    task_queue  true	0	0	0
    waring_queue    true	19	0	19
    ```
## 总结
使用java客户端，创建ConnectionFactory对象，设置连接属性后，创建已经到服务到Connection，Connection代表一个Tcp连接，可以在客户端复用，复用方式
为使用Connection创建Channel。可以根据规划事先在服务端创建好交换器、队列等资源，也可以在Channel上声明这些资源，声明资源时，如果资源不存在则会创建。
一切资源就绪，生产者在Channel使用basicPublish发送消息到指定到交换器，消费这订阅指定的队列，定义消费消息的回调函数，在Channel上使用basicConsume消费消息。