import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;

public class ChannelFactory extends BasePooledObjectFactory<Channel> {
    ConnectionFactory factory;
    Connection connection;

    public ChannelFactory() {
        this.factory = new ConnectionFactory();
        factory.setHost("54.187.241.134");
        factory.setUsername("guest");
        factory.setPassword("guest");
        try  {
            this.connection = factory.newConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Channel create() throws IOException {
        return connection.createChannel();
    }

    /**
     * Use the default PooledObject implementation.
     */
    @Override
    public PooledObject<Channel> wrap(Channel channel) {
        return new DefaultPooledObject<>(channel);
    }
}

