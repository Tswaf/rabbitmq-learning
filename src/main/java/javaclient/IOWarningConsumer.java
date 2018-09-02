package javaclient;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

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
