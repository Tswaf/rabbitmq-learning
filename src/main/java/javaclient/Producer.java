package javaclient;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Producer {
    public static String EXCHANEG = "alert_exchange";
    public static String WARNING_QUEUE = "waring_queue";
    public static String ERROR_QUEUE = "error_queue";

    public static void main(String[] args) throws IOException, TimeoutException {
        //get a default connection and create a channel
        ConnectionFactory cf = new ConnectionFactory();
        Connection connection = cf.newConnection();
        final Channel channel = connection.createChannel();

        //declare exchange,queues and bindings
        channel.exchangeDeclare(EXCHANEG, BuiltinExchangeType.TOPIC);
        channel.queueDeclare(WARNING_QUEUE,true,false,false,null);
        channel.queueDeclare(ERROR_QUEUE,true,false,false,null);
        channel.queueBind(WARNING_QUEUE, EXCHANEG,"*.warning");
        channel.queueBind(ERROR_QUEUE, EXCHANEG,"*.error");

        while (true){
            String key = randomRoutingKey();
            String message = message(key);
            channel.basicPublish(EXCHANEG,key,null,message.getBytes());
            System.out.println("Sent > " + message);
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static String[] keys = new String[]{"io.error","io.warning","file.error","file.warning"};

    public static String randomRoutingKey(){
        Random random = new Random();
        int rand = Math.abs(random.nextInt())%(keys.length);
        return keys[rand];
    }

    public static String message(String key){
        String message = "Alert ..." + " level:" + key;
        return message;
    }
}
