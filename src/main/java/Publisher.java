import Entities.LiftRideLocal;
import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.MessageProperties;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;

public class Publisher {
  private ObjectPool<Channel> channelPool;
  private String QUEUE_NAME;

  public Publisher(String queueName) throws IOException, TimeoutException {
    this.channelPool = new GenericObjectPool<>(new ChannelFactory());
    this.QUEUE_NAME = queueName;
  }

  public void send(LiftRideLocal liftRide) {
    Channel channel = null;
    try {
      channel = channelPool.borrowObject();
      channel.queueDeclare(QUEUE_NAME, true, false, false, null);
      Gson gson = new Gson();
      channel.basicPublish("", QUEUE_NAME, MessageProperties.PERSISTENT_TEXT_PLAIN, gson.toJson(liftRide).getBytes(StandardCharsets.UTF_8));
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      try {
        if(channel != null) {
          channelPool.returnObject(channel);
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
}