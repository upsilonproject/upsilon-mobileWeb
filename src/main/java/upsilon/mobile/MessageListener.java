package upsilon.mobile;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BasicProperties;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.QueueingConsumer;

import java.util.Date;

import static com.rabbitmq.client.AMQP.Queue.DeclareOk;

public class MessageListener implements Runnable {
    private MainActivity mainActivity;

    public interface Listener {
        public void onConnected();
        public void onDisconnected();
        public void onError(String message);
    }

    private Listener listener;

    public void addListener(Listener l) {
        this.listener = l;
    }

    public boolean isConnected() {
        return conn.isOpen();
    }

    private Connection conn;
    private Channel channel;

    public void run() {
        ConnectionFactory factory = new ConnectionFactory();

        try {
            conn = factory.newConnection();

            if (conn.isOpen()) {
                listener.onConnected();
            }

            this.channel = conn.createChannel();
            this.channel.basicQos(1);

            DeclareOk q = this.channel.queueDeclare();

            this.channel.queueBind(q.getQueue(), "ex_upsilon", "upsilon.notifications");

            QueueingConsumer consumer = new QueueingConsumer(this.channel);
            this.channel.basicConsume(q.getQueue(), true, consumer);

            while (true) {
                QueueingConsumer.Delivery delivery = consumer.nextDelivery();

                mainActivity.speak(new String(delivery.getBody()));

                this.channel.basicAck(delivery.getEnvelope().getDeliveryTag(), true);
            }
        } catch (Exception e) {
            mainActivity.alert("amqp exception", e.getMessage());
            listener.onError(e.getMessage());
        }

        listener.onDisconnected();
    }

    public void sendHeartbeat() {
        AMQP.BasicProperties properties = new AMQP.BasicProperties();

        byte[] body = new Date().toString().getBytes();
        try {
            this.channel.basicPublish("ex_publish", "upsilon.heartbeats", properties, body);
        } catch (Exception e) {
            this.listener.onError(e.getMessage());
        }
    }

    public void reconnect() {
        if (this.conn.isOpen()) {
            try {
                this.conn.close();
            } catch (Exception e) {

            }
        }

        new Thread(this, "conn thread");
    }
}
