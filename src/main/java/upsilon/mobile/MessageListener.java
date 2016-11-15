package upsilon.mobile;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.QueueingConsumer;

import static com.rabbitmq.client.AMQP.Queue.DeclareOk;

public class MessageListener implements Runnable {
    private MainActivity mainActivity;

    public MessageListener(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    public void run() {
        ConnectionFactory factory = new ConnectionFactory();
        try {
            Connection conn = factory.newConnection();
            Channel channel = conn.createChannel();
            channel.basicQos(1);

            DeclareOk q = channel.queueDeclare();

            channel.queueBind(q.getQueue(), "ex_upsilon", "upsilon.notifications");

            QueueingConsumer consumer = new QueueingConsumer(channel);
            channel.basicConsume(q.getQueue(), true, consumer);

            while (true) {
                QueueingConsumer.Delivery delivery = consumer.nextDelivery();

                mainActivity.speak(new String(delivery.getBody()));

                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), true);
            }
        } catch (Exception e) {
            mainActivity.alert("amqp exception", e.getMessage());
        }

    }
}
