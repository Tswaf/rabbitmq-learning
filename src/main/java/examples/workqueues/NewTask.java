package examples.workqueues;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;

import java.io.IOException;
import java.util.concurrent.TimeoutException;


public class NewTask {
    private static final String TASK_QUEUE_NAME = "task_queue";

    public static void main(String[] args) throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.queueDeclare(TASK_QUEUE_NAME,true,false,false,null);

        String message =getMessage(args);

        channel.basicPublish("",TASK_QUEUE_NAME, MessageProperties.PERSISTENT_TEXT_PLAIN,message.getBytes("UTF-8"));
        System.out.println(" [x] Sent '" + message + "'");

        channel.close();
        connection.close();
    }

    private static String getMessage(String[] args) {
        if (args.length < 1) {
            return "Hello World";
        }
        return joinStrings(args," ");
    }

    private static String joinStrings(String[] args,String delimiter) {
        int length = args.length;
        if (length==0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(args[0]);
        for (int i=1; i<length; i++) {
            sb.append(delimiter).append(args[i]);
        }
        return sb.toString();
    }
}
