package confirm;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class ProduceWaitConfirm {
    public static void main(String[] args) throws IOException, TimeoutException {
        ConnectionFactory cf = new ConnectionFactory();
        Connection connection = cf.newConnection();
        Channel channel = connection.createChannel();

        channel.exchangeDeclare("cfmExchange","fanout");
        channel.queueDeclare("cfmQueue",false,false,false,null);
        channel.queueBind("cfmQueue","cfmExchange","");

        try {
            channel.confirmSelect();
            channel.basicPublish("cfmExchange","",null,"msg".getBytes());
            if(!channel.waitForConfirms()) {
                System.out.println("send fail");
            }else{
                System.out.println("send success");
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        channel.close();
        connection.close();
    }
}
