package unroutabel;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Return;
import com.rabbitmq.client.ReturnCallback;
import com.rabbitmq.client.ReturnListener;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
