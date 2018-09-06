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
### 消息的TTL
### 队列的TTL

## 死信