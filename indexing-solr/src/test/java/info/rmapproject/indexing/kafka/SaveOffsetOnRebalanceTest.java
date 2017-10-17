package info.rmapproject.indexing.kafka;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SaveOffsetOnRebalanceTest {

    @Test(expected = IllegalArgumentException.class)
    public void testNullOffsetLookupOnConstruction() throws Exception {
        new SaveOffsetOnRebalance(null);
    }

    @Test(expected = IllegalArgumentException.class)
    @SuppressWarnings("unchecked")
    public void testNullConsumerOnConstruction() throws Exception {
        new SaveOffsetOnRebalance(mock(OffsetLookup.class), null);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testNonNullConstructors() throws Exception {
        new SaveOffsetOnRebalance(mock(OffsetLookup.class));
        new SaveOffsetOnRebalance(mock(OffsetLookup.class), mock(Consumer.class));
    }

    @Test(expected = IllegalArgumentException.class)
    @SuppressWarnings("unchecked")
    public void testSetNullConsumer() throws Exception {
        SaveOffsetOnRebalance underTest = new SaveOffsetOnRebalance(mock(OffsetLookup.class), mock(Consumer.class));
        underTest.setConsumer(null);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSetNonNullConsumer() throws Exception {
        SaveOffsetOnRebalance underTest = new SaveOffsetOnRebalance(mock(OffsetLookup.class), mock(Consumer.class));
        underTest.setConsumer(mock(Consumer.class));
    }

    @Test
    public void testDefaultSeekBehavior() throws Exception {
        SaveOffsetOnRebalance underTest = new SaveOffsetOnRebalance(mock(OffsetLookup.class));
        assertEquals(SaveOffsetOnRebalance.DEFAULT_SEEK_BEHAVIOR, underTest.getSeekBehavior());
    }

    @Test
    public void testSetSeekBehavior() throws Exception {
        SaveOffsetOnRebalance underTest = new SaveOffsetOnRebalance(mock(OffsetLookup.class));

        underTest.setSeekBehavior(Seek.LATEST);
        assertEquals(Seek.LATEST, underTest.getSeekBehavior());

        underTest.setSeekBehavior(Seek.EARLIEST);
        assertEquals(Seek.EARLIEST, underTest.getSeekBehavior());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testOnPartitionsRevoked() throws Exception {
        String topic = "topic";
        int partition = 0;
        long offset = 21;
        TopicPartition tp = new TopicPartition(topic, partition);
        OffsetAndMetadata commitOffsetMd = new OffsetAndMetadata(offset, null);
        Consumer consumer = mock(Consumer.class);
        OffsetLookup lookup = mock(OffsetLookup.class);
        SaveOffsetOnRebalance underTest = new SaveOffsetOnRebalance(lookup, consumer);

        when(consumer.position(tp)).thenReturn(offset);

        underTest.onPartitionsRevoked(Collections.singleton(tp));

        verify(consumer).position(tp);
        verify(consumer).commitSync(new HashMap(){
                {
                    put(tp, commitOffsetMd);
                }
        });
    }

    @Test
    public void testOnPartitionsAssigned() throws Exception {

    }
}
