package deadletter;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class Publisher {
    private static final String ORG_EXCHANGE = "org_ex";
    private static final String DLX_EXCHANGE = "dlx_ex";
    private static final String DL_QUEUE = "dl_queue";
    private static final String BAK_QUEUE = "bk_queue";

    public static void main(String[] args) throws IOException, TimeoutException {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        Connection connection = connectionFactory.newConnection();
        Channel channel = connection.createChannel();

        channel.exchangeDeclare(ORG_EXCHANGE,"fanout");

        Map<String,Object> params = new HashMap<String, Object>();
        params.put("x-message-ttl",10000);
        params.put("x-dead-letter-exchange",DLX_EXCHANGE);
        params.put("x-dead-letter-routing-key","deadKey");
        channel.queueDeclare(DL_QUEUE,false,false,false,params);
        channel.queueBind(DL_QUEUE,ORG_EXCHANGE,"");

        channel.exchangeDeclare(DLX_EXCHANGE,"fanout");
        channel.queueDeclare(BAK_QUEUE,false,false,false,null);
        channel.queueBind(BAK_QUEUE,DLX_EXCHANGE,"");

        channel.basicPublish(ORG_EXCHANGE,"normalKey",null,"message1".getBytes());
        channel.basicPublish(ORG_EXCHANGE,"deadKey",null,"message2".getBytes());


        channel.close();
        connection.close();
    }
}
