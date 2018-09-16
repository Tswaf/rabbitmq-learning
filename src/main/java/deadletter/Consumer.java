package deadletter;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.Map;

public class Consumer {
    private static final String BAK_QUEUE = "bk_queue";

    public static void main(String[] args) throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        com.rabbitmq.client.Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
                    throws IOException {
                String message = new String(body);

                System.out.println("======message===============");
                System.out.println("body: " + message);
                System.out.println("routingkey: " + envelope.getRoutingKey());
                System.out.println("fromExchange: " +envelope.getExchange());

                Map<String,Object> headers = properties.getHeaders();
                System.out.println(headers.get("x-death"));

                System.out.println("----------------------------");

            }
        };

        channel.basicConsume(BAK_QUEUE,true,consumer);
    }
}
