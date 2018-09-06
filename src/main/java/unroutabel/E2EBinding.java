package unroutabel;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class E2EBinding {
    public static String SOURCE_EXCHANGE = "source_ex";
    public static String DESTINATION_EXCHANGE = "dest_ex";
    public static String QUEUE = "bak_queue";
    public static String ROUTING_KEY = "bak";

    public static void main(String[] args) throws IOException, TimeoutException {
        ConnectionFactory  cf = new ConnectionFactory();
        Connection connection = cf.newConnection();
        Channel channel = connection.createChannel();

        channel.exchangeDeclare(SOURCE_EXCHANGE, BuiltinExchangeType.DIRECT);
        channel.exchangeDeclare(DESTINATION_EXCHANGE,BuiltinExchangeType.FANOUT);
        channel.queueDeclare(QUEUE,false,false,false,null);

        channel.exchangeBind(DESTINATION_EXCHANGE,SOURCE_EXCHANGE,ROUTING_KEY);
        channel.queueBind(QUEUE,DESTINATION_EXCHANGE,ROUTING_KEY);


        channel.basicPublish(SOURCE_EXCHANGE,ROUTING_KEY,null,"message".getBytes());


    }
}
