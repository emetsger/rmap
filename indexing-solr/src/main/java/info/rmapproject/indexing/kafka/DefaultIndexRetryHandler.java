package info.rmapproject.indexing.kafka;

import info.rmapproject.indexing.IndexingTimeoutException;
import info.rmapproject.indexing.solr.repository.CustomRepo;
import info.rmapproject.indexing.solr.repository.IndexDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static info.rmapproject.indexing.solr.IndexUtils.assertNotNull;
import static info.rmapproject.indexing.solr.IndexUtils.assertPositive;
import static info.rmapproject.indexing.solr.IndexUtils.iae;
import static java.lang.System.currentTimeMillis;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class DefaultIndexRetryHandler implements IndexingRetryHandler {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultIndexRetryHandler.class);

    private int indexRetryTimeoutMs;

    private float indexRetryBackoffFactor;

    private int indexRetryMaxMs;

    private CustomRepo repository;

    public DefaultIndexRetryHandler(CustomRepo repository, int indexRetryTimeoutMs, float indexRetryBackoffFactor,
                                    int indexRetryMaxMs) {

        this.indexRetryTimeoutMs = assertPositive(indexRetryTimeoutMs,
                iae("Index retry timeout must be a positive integer."));
        this.indexRetryBackoffFactor = assertPositive(indexRetryBackoffFactor,
                iae("Index retry backoff factor must be a positive float greater than one."));
        this.indexRetryMaxMs = assertPositive(indexRetryMaxMs,
                iae("Index retry max ms must be a positive integer."));
        this.repository = assertNotNull(repository, iae("Repository must not be null."));

        validateRetryMaxMs(indexRetryTimeoutMs, indexRetryMaxMs,
                "Index retry max ms must be equal or greater than index retry timeout");

    }

    @Override
    public void retry(IndexDTO dto) throws IndexingTimeoutException {
        long start = currentTimeMillis();
        int attempt = 1;
        long timeout = indexRetryTimeoutMs;
        boolean success = false;
        Exception retryFailure = null;

        do {

            try {
                LOG.debug("Retry attempt {} (total elapsed time {} ms) indexing {}",
                        attempt, (currentTimeMillis() - start), dto);
                repository.index(dto);
                success = true;
            } catch (Exception e) {
                retryFailure = e;
                LOG.debug("Retry attempt {} failed: {}", e.getMessage(), e);
            }

            if (!success) {
                try {
                    Thread.sleep(timeout);
                } catch (InterruptedException e) {
                    Thread.interrupted();
                }
                attempt++;
                timeout = Math.round(timeout * indexRetryBackoffFactor);
            }

        } while ((currentTimeMillis() - start) < indexRetryMaxMs && !success);

        if (!success) {
            String fmt = "Timeout after %s attempts, %s ms: failed to index %s: %s";
            String msg = String.format(fmt, attempt, (currentTimeMillis() - start), dto, retryFailure.getMessage());
            LOG.error(msg);
            throw new IndexingTimeoutException(msg, retryFailure);
        }
    }

    public int getIndexRetryTimeoutMs() {
        return indexRetryTimeoutMs;
    }

    public void setIndexRetryTimeoutMs(int indexRetryTimeoutMs) {
        validateRetryMaxMs(indexRetryTimeoutMs, indexRetryMaxMs,
                "Retry max ms must be a positive integer and be greater than the retry timeout.");
        this.indexRetryTimeoutMs = assertPositive(indexRetryTimeoutMs,
                iae("Index retry timeout must be a positive integer."));
    }

    public float getIndexRetryBackoffFactor() {
        return indexRetryBackoffFactor;
    }

    public void setIndexRetryBackoffFactor(float indexRetryBackoffFactor) {
        this.indexRetryBackoffFactor = assertPositive(indexRetryTimeoutMs,
                iae("Index retry backoff factor must be a positive float greater than one."));
    }

    public int getIndexRetryMaxMs() {
        return indexRetryMaxMs;
    }

    public void setIndexRetryMaxMs(int indexRetryMaxMs) {
        validateRetryMaxMs(1, indexRetryMaxMs,
                "Retry max ms must be a positive integer and be greater than the retry timeout.");
        this.indexRetryMaxMs = assertPositive(indexRetryMaxMs,
                iae("Index retry max ms must be a positive integer."));
    }

    public CustomRepo getRepository() {
        return repository;
    }

    public void setRepository(CustomRepo repository) {
        assertNotNull(repository, iae("Repository must not be null."));
        this.repository = repository;
    }

    private static void validateRetryMaxMs(int indexRetryTimeoutMs, int indexRetryMaxMs, String s) {
        if (indexRetryMaxMs < indexRetryTimeoutMs) {
            throw new IllegalArgumentException(s);
        }
    }

}
