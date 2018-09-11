# 未路由的消息、TTL和死信
## 未路由的消息
当生产这发送的消息到达指定的交换器后，如果交换器无法根据自身类型、绑定的队列以及消息的路由键找到匹配的队列，默认情况下消息将被丢弃。可以通过两种方式
处理这种情况，一是在发送是设置mandatory参数，二是通过备份交换器。

### 设置mandatory参数
在发送消息是，可以设置mandatory参数未true，这样当消息在交换器上无法被路由时，服务器将消息返回给生产者，生产者实现回调函数处理被服务端返回的消息。  
```java
public class NoRouteMessage {
    private static String QUEUE = "unreachable_queue";
    private static String EXCHANGE = "unreachable_exchange";
    private static String BINDING_KEY = "fake_key";

    public static void main(String[] args) throws IOException, TimeoutException {
        ConnectionFactory cf = new ConnectionFactory();
        Connection connection = cf.newConnection();
        Channel channel = connection.createChannel();

        channel.exchangeDeclare(EXCHANGE, BuiltinExchangeType.DIRECT,false);
        channel.queueDeclare(QUEUE,false,false,false,null);
        channel.queueBind(QUEUE,EXCHANGE,BINDING_KEY);

        String message = "an unreachable message";
        boolean mandatory = true;
        channel.basicPublish(EXCHANGE,"mykey",mandatory,null,message.getBytes());

        channel.addReturnListener(new ReturnListener() {
            public void handleReturn(int replyCode, String replyText, String exchange, String routingKey, AMQP.BasicProperties properties, byte[] body) throws IOException {
                System.out.println("replyCode: " + replyCode);
                System.out.println("replyText: " + replyText);
                System.out.println("exchange: " + exchange);
                System.out.println("routingKey: " + routingKey);
                System.out.println("message: " + new String(body));
            }
        });

        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        channel.close();
        connection.close();

    }
}
```
如上代码所示，创建了队列并和direct类型的交换器使用"fake_key"绑定，发送消息时,设定消息路由键为"mykey"，这样消息到达交换器时将无比被路由。由于发送消息时
设置basicPublish的参数为true，并为生产这添加处理返回消息的回调方法，这样，消息将被服务端返回并在回调中得到处理。

### 备份交换器
与设置mandatory将无法路由的消息返回给生产者不同，可以为交换器设置一般备份交换器（Alternate Exchange）,这样，消息在交换器上无法路由时，将被直接发送到
备份交换器，由备份交换器再次路由。
在下面到示例中，创建了交换器source_exchange，生产者将消息发送到该交换器。source_exchange并未绑定任何队列，这将导致消息被丢弃。为了处理这种情况，创建
了交换器ae并绑定了一个队列，然后将ae作为source_exchange对备份交换器，这是通过创建source_exchange交换器时设定alternate-exchange参数完成的。之后，发送到
source_exchange到消息将被服务端发送到ae交换器中，然后路由到ae_queue等待处理。

```java
public class AlternateExchange {
    private static String QUEUE = "ae_queue";
    private static String EXCHANGE = "source_exchange";
    private static String AE = "ae";

    private static String BINDING_KEY = "fake_key";

    public static void main(String[] args) throws IOException, TimeoutException {
        ConnectionFactory cf = new ConnectionFactory();
        Connection connection = cf.newConnection();
        Channel channel = connection.createChannel();

        channel.exchangeDeclare(AE, "fanout");

        Map<String,Object> exArgs = new HashMap<String, Object>();
        exArgs.put("alternate-exchange",AE);
        channel.exchangeDeclare(EXCHANGE,"direct",false,false,exArgs);

        channel.queueDeclare(QUEUE,false,false,false,null);

        channel.queueBind(QUEUE,AE,"");

        channel.basicPublish(EXCHANGE,"anyKey",null,"message".getBytes());

    }
}
```

## TTL
在RabbitMQ中，可以为消息和队列设置过期时间。消息过期未被消费后，默认被丢弃；队列过期也会被删除。
### 消息的TTL
可以通过两种方式来为消息设置TTL，一是在发送消息是设置单条消息的TTL；二是在队列上通过队列属性设置TTL，这种情况下，路由到该队列到消息都拥有同样都TTL。
当然，也可以同时使用两种方式，这时，消息的TTL取两者中较小的。
1. 设置单条消息都TTL
    使用basic.Publish发送消息时，通过expiration参数设置消息的TTL。
    
    ```java
    AMQP.BasicProperties.Builder builder = new AMQP.BasicProperties.Builder();
    builder.expiration("10000"); //ttl 10s
    AMQP.BasicProperties properties = builder.build();
    channel.basicPublish(EXCHANGE,"",properties,"10s TTL message".getBytes());
    ```

2. 通过队列属性设置TTL
    创建队列时，可以通过队列的*x-message-ttl*参数来设置队列中消息的TTL。
    
    ```java
    Map<String,Object> params = new HashMap<String, Object>();
    params.put("x-message-ttl",5000);
    channel.queueDeclare(QUEUE,false,false,false,params);
    ```    
    上述代码将队列的消息ttl设置为5s。

对于第二种在队列上设置消息TTL到方式，消息一旦过期，会立刻被从队列中删除；而通过第一种发送消息时设置TTL的方式，消息过期后不一定会立即删除。这是由内部实现决定的，
对于第二种方式，队列中消息的TTL都相同，则消息过期顺序和入队顺序一致，那么只需要从队头定期删除消息即可；而第一种方式下，每条消息过期时间都不同，要实现"实时"删除
过期消息，得不断扫描整个队列，代价太大，所以等到消息即将被推送给消费者时在判断是否过期，如果过期就删除，是一种惰性处理策略。
#### 示例
在以下示例中，创建来一个队列，并设置其中的消息TTL为20s，然后发送两条被路由到该队列的消息。第一条消息发送时设置了TTL为10s，这样，它到达队列后的TTL将为10s；
第二条消息发送时未设置TTL，它到达队列后的TTL为20s。

```java
channel.exchangeDeclare(EXCHANGE,"fanout");

Map<String,Object> params = new HashMap<String, Object>();
params.put("x-message-ttl",20000);
channel.queueDeclare(QUEUE,false,false,false,params);

channel.queueBind(QUEUE,EXCHANGE,"");

AMQP.BasicProperties.Builder builder = new AMQP.BasicProperties.Builder();
builder.expiration("10000"); //ttl 10s
AMQP.BasicProperties properties = builder.build();
channel.basicPublish(EXCHANGE,"",properties,"10s TTL message".getBytes());

channel.basicPublish(EXCHANGE,"",null,"20s TTL message".getBytes());
```
可以在RabbitMQ的Web管理页面或使用rabbitmqctl工具在命令行中看到，队列中到消息刚开时积攒了两条，10秒钟后第一条消息到达TTL未被消费，被从队列中丢弃，队列中
只剩第二条消息，在过10s，第二条消息也不丢弃。
### 队列的TTL
与消息TTL类型，可以为队列设置TTL。为队列中设置了TTL后，如果TTL时间内队列上没有消费者，或者队列没有被重新声明，那么队列将被服务端自动删除。
使用basic.QueueDeclare(channel.queueDeclare)声明队列时，通过*x-expires*参数可以设置队列的TTL。  
声明一个ttl为10s的队列：

```java
Map<String,Object> qArgs = new HashMap<String, Object>();
qArgs.put("x-expires",10000);
channel.queueDeclare(TEMP_QUEUE,false,false,false,qArgs);
```
## 死信