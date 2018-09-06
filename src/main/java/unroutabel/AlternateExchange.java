package unroutabel;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

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
