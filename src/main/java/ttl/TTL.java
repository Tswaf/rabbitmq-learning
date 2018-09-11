package ttl;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class TTL {
    private static String QUEUE = "ttl_queue";
    private static String EXCHANGE = "ttl_exchange";
    private static String TEMP_QUEUE = "temp_queue";


    public static void main(String[] args) throws IOException, TimeoutException {
        ConnectionFactory cf = new ConnectionFactory();
        Connection connection = cf.newConnection();
        Channel channel = connection.createChannel();

        channel.exchangeDeclare(EXCHANGE,"fanout");

        //declare a queue with message ttl 20s
        Map<String,Object> params = new HashMap<String, Object>();
        params.put("x-message-ttl",20000);
        channel.queueDeclare(QUEUE,false,false,false,params);

        channel.queueBind(QUEUE,EXCHANGE,"");

        //publish a message with 10s ttl
        AMQP.BasicProperties.Builder builder = new AMQP.BasicProperties.Builder();
        builder.expiration("10000"); //ttl 10s
        AMQP.BasicProperties properties = builder.build();
        channel.basicPublish(EXCHANGE,"",properties,"10s TTL message".getBytes());

        //publish a message without ttl
        channel.basicPublish(EXCHANGE,"",null,"20s TTL message".getBytes());


        //declare a queue with ttl 10s
        Map<String,Object> qArgs = new HashMap<String, Object>();
        qArgs.put("x-expires",10000);
        channel.queueDeclare(TEMP_QUEUE,false,false,false,qArgs);

        channel.close();
        connection.close();


    }
}
