package transaction;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class TxClient {
    public static void main(String[] args) throws IOException, TimeoutException {
        ConnectionFactory cf = new ConnectionFactory();
        Connection connection = cf.newConnection();
        Channel channel = connection.createChannel();

        channel.exchangeDeclare("txExchange","fanout");
        channel.queueDeclare("txQueue",false,false,false,null);
        channel.queueBind("txQueue","txExchange","");

        channel.txSelect();
        channel.basicPublish("txExchange","",null,"m1".getBytes());
        channel.basicPublish("txExchange","",null,"m2".getBytes());
        channel.txCommit();


        channel.txSelect();
        try {
            channel.basicPublish("txExchange","",null,"m3".getBytes());
            int a = 1/0;
            channel.txCommit();
        }catch (Exception e){
            e.printStackTrace();
            channel.txRollback();
        }

        channel.close();
        connection.close();
    }
}
