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
    public interface Listener {
        public void onConnected();
        public void onDisconnected();
        public void onError(String message);
        public void onNotification(String message);
    }

    private Listener listener;

    public void addListener(Listener l) {
        this.listener = l;
    }

    private Thread connThread;

    public boolean isConnected() {
        if (this.conn == null) {
            return false;
        }

        return conn.isOpen();
    }

    private Connection conn;
    private Channel channel;

    public void run() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("upsilon.teratan.net");

        try {
            this.conn = factory.newConnection();

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

                listener.onNotification(new String(delivery.getBody()));
            }
        } catch (Exception e) {
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
        if (this.conn != null) {
            if (this.conn.isOpen()) {
                try {
                    this.conn.close();
                } catch (Exception e) {

                }
            }
        }

        this.connThread = new Thread(this, "conn thread");
        this.connThread.start();
    }
}
