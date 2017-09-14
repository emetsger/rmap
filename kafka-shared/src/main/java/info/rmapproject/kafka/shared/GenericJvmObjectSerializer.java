package info.rmapproject.kafka.shared;

import org.apache.kafka.common.serialization.Serializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Map;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class GenericJvmObjectSerializer<T> implements Serializer<T> {

    public GenericJvmObjectSerializer() {

    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        // no-op
    }

    @Override
    public byte[] serialize(String topic, T data) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
                oos.writeObject(data);
            }
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Error serializing an instance of " + data.getClass().getName() + ": " +
                    e.getMessage(), e);
        }
    }

    @Override
    public void close() {
        // no-op
    }

}
