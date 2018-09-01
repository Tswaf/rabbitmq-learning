package javaclient;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class Connect {
    public static void main(String[] args) throws IOException, TimeoutException {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost("localhost");
        connectionFactory.setPort(5672);
        connectionFactory.setUsername("guest");
        connectionFactory.setPassword("guest");
        connectionFactory.setVirtualHost("/");

        Connection connection = connectionFactory.newConnection();
        final Channel channel = connection.createChannel();

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
                        //process(body,consumerTag,envelope,properties) //消费消息
                        channel.basicAck(deliveryTag,false);
                    }
                });
    }
}
