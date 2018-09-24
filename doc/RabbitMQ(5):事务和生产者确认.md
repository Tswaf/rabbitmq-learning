# 事务&生产者确认
一般情况下，生产者将消息发送后，继续进行别的业务逻辑处理。消息从生产者发送后，可能由于网络原因丢失，也可能因为RabbitMQ服务端奔溃未被处理...总之,对于
消息是否安全到达服务器，生产者一无所知。在一些场景下，这也许是可行的，毕竟丢失消息的几率较小，丢失几条消息也并不会产生严重的后果；在另外一些场景，需要生产
者确认消息到达服务端，或者得知消息未到达服务端的情况以做出相应的处理。在RabbitMQ中，生产者可以通过两种方式达到上述目的：事务和生产者确认。

## 事务
RabbitMQ事务的概念并不模式，与数据库事务大同小异。在RabbitMQ中，客户端开启事务，之后开始发送消息，此时的消息并未达到服务端，直到提交事务，如果事务提交成功，则之前发送的消息被
发送的服务端，否则服务端抛出异常，我们捕获该异常并进行事务回滚。  
事务机制是在AMQP协议层面支持的，对应到RabbitMQ中，通过以下方法进行支持：

```java
channel.txSelect() //将信道置为信道模式，开启事务
channel.txCommit() //提交事务
channel.txRollback() //回滚事务
```

事务的一般使用方式如下：

```java
channel.txSelect(); //开始事务
try {
    channel.basicPublish("txExchange","",null,"m3".getBytes()); //发送一条或多条消息
    //...
    channel.txCommit(); //提交事务
}catch (Exception e){
    e.printStackTrace();
    channel.txRollback(); //回滚事务
}
```

事务纵然可以保证生产者消息到达服务端，然而这是以性能为代价的。事务会阻塞发送方，直到RabbitMQ回应后，才可以继续发送消息，大量的使用事务机制会严重拖垮服务端的性能。

## 发送方确认
与事务机制相比，发送方确认显得更加轻量级。  
在发送方确认机制中，发送方发送完消息后，继续别的业务处理；服务端稍后会发送给发送方ack或者nack消息，表明服务端已经接收到来消息还或者由于自身原因无法处理消息；发送方接收到
服务端到响应后，进行响应的处理。可见，发送方确认机制采用来一中异步的处理方式。  
问题在于，发送方和服务端如何唯一确定一条消息呢？如果服务端返回了ack响应，它是对哪条消息的ack呢？也就是说在发送方确认机制下，需要来标志一条消息的唯一性。
一旦开启来发送方确认机制，信道上的发送消息将被从1编号，每条消息都将拥有一个唯一都编号，之后服务端响应时，使用deliveryTag来告诉发送方，它响应都是哪一条消息。  
需要注意的是，编号是channel级别的，这样做能保证消息编号唯一性的关键在于，channel不是多线程共享的，发送方应该使用单一线程在channel发送消息来保证消息编号的唯一性，之后再
在该channel处理服务端的响应。  
尽管消息被在channel上自动编号，但这只是RabbitMQ服务端和发送方确定消息唯一性的手段。对于业务而言，如果收到一条服务端的nack响应，告诉发送方eliveryTag=5，
发送方如何处理呢？也许它需要重新发送消息，但它只知道deliveryTag=5，5号消息是什么消息呢？也就是说，仍然需要客户端维护消息状态，使用发送方确认机制时，
发送方仍然可能需要维护一个消息的集合，记录已经被发送的消息，之后收到服务端的ack后，再从集合中删除消息，或者收到nack时，决定重新发送或是别的处理，总之，
发送方维护了消息集合，之后才有可能根据服务端返回deliveryTag，从集合中获得具体的消息。  

如何使用呢？  
首先，调用channel.confirmSelect将开启发送方确认：  

```java
channel.confirmSelect()
```
此后，信息被设置成confirm模式，发送方开始发送消息。  
发送方有两种方式来处理服务端的响应：   

- 调用channel.waitForConfirms()等待服务端响应
    该调用会等待直到服务的响应，如果在该方法上次被调用之后，所以发送的消息都被服务端ack，则返回true，否则返回false。如果再次之前信道未被设置成confirm
    模式，那么waitForConfirms调用会抛出异常。使用waitForConfirms的示例如下：

    ```java
    try {
        channel.confirmSelect();
        channel.basicPublish("cfmExchange","",null,"msg".getBytes());
        if(channel.waitForConfirms()) {
            System.out.println("send success");
        } else {
            System.out.println("send fail");
        }
     } catch (InterruptedException e) {
        e.printStackTrace();
     }
     ```

- 定义监听回调函数，处理服务端响应

    ```java
    channel.addConfirmListener(new ConfirmListener() {
        public void handleAck(long deliveryTag, boolean multiple) throws IOException {

        }

        public void handleNack(long deliveryTag, boolean multiple) throws IOException {

        }
    });

    ```
    handleAck和handleNack分别处理被服务端ack和nack的消息；deliveryTag为消息编号，multiple设置为true是，一次性处理多条消息，即编号消息deliveryTag的消息。
    
