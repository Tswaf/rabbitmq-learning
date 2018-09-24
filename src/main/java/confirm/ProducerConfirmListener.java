package confirm;

import com.rabbitmq.client.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConfirmListener;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ProducerConfirmListener {
    private static Map<Long,String> msgs = new HashMap<Long, String>();
    public static void main(String[] args) throws IOException, TimeoutException {
        ConnectionFactory cf = new ConnectionFactory();
        Connection connection = cf.newConnection();
        Channel channel = connection.createChannel();

        channel.exchangeDeclare("cfmExchange","fanout");
        channel.queueDeclare("cfmQueue",false,false,false,null);
        channel.queueBind("cfmQueue","cfmExchange","");

        channel.confirmSelect();
        channel.addConfirmListener(new ConfirmListener() {
            public void handleAck(long deliveryTag, boolean multiple) throws IOException {
                System.out.println("ackedByBroker,deliveryTag:" + deliveryTag + ",msg:" + msgs.get(deliveryTag));
                msgs.remove(deliveryTag);
            }

            public void handleNack(long deliveryTag, boolean multiple) throws IOException {
                System.out.println("nackedByBroker,deliveryTag:" + deliveryTag + ",msg:" + msgs.get(deliveryTag));
            }
        });

        while (true){
            long seqNo = channel.getNextPublishSeqNo();
            String msg = "message" + seqNo;
            channel.basicPublish("cfmExchange","",null,msg.getBytes());
            msgs.put(seqNo,msg);
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
