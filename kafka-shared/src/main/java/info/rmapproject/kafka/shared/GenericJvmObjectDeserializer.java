package info.rmapproject.kafka.shared;

import org.apache.kafka.common.serialization.Deserializer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Map;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class GenericJvmObjectDeserializer<T> implements Deserializer<T> {

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        // no-op
    }

    @Override
    @SuppressWarnings("unchecked")
    public T deserialize(String topic, byte[] bytes) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
             ObjectInputStream ois = new ObjectInputStream(bais)) {
                return (T) ois.readObject();
        } catch (ClassNotFoundException|IOException e) {
            throw new RuntimeException("Error deserializing a byte stream to a Java object: " + e.getMessage(), e);
        }
    }

    @Override
    public void close() {
        // no-op
    }

}
