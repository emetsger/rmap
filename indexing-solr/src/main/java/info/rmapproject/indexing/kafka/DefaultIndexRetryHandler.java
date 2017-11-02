package info.rmapproject.indexing.kafka;

import info.rmapproject.indexing.IndexingInterruptedException;
import info.rmapproject.indexing.IndexingTimeoutException;
import info.rmapproject.indexing.solr.model.DiscoSolrDocument;
import info.rmapproject.indexing.solr.repository.EventTupleIndexingRepository;
import info.rmapproject.indexing.solr.repository.IndexDTO;
import info.rmapproject.indexing.solr.repository.IndexDTOMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

import static info.rmapproject.indexing.IndexUtils.assertNotNull;
import static info.rmapproject.indexing.IndexUtils.assertPositive;
import static info.rmapproject.indexing.IndexUtils.iae;
import static java.lang.System.currentTimeMillis;

/**
 * Attempts to index a {@link IndexDTO}, retrying until the operation succeeds or a timeout is exceeded.
 *
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class DefaultIndexRetryHandler implements IndexingRetryHandler {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultIndexRetryHandler.class);

    private int indexRetryTimeoutMs;

    private float indexRetryBackoffFactor;

    private int indexRetryMaxMs;

    private EventTupleIndexingRepository<DiscoSolrDocument> repository;

    private IndexDTOMapper dtoMapper;

    /**
     * Retry indexing operations every {@code (indexRetryMs * indexRetryBackoffFactor)} ms, up to
     * {@code indexRetryMaxMs}.
     * Constructs a retry handler with:
     * <dl>
     *     <dt>indexRetryTimeoutMs</dt>
     *     <dd>100</dd>
     *     <dt>indexRetryMaxMs</dt>
     *     <dd>120000</dd>
     *     <dt>indexRetryBackoffFactor</dt>
     *     <dd>1.5</dd>
     * </dl>
     *
     * @param repository the Solr repository, must not be {@code null}
     * @param dtoMapper maps {@code IndexDTO} objects to a stream of {@code EventDiscoTuple}
     * @throws IllegalArgumentException if {@code repository} is {@code null}, if any time-out related parameter is not
     *                                  1 or greater, if {@code indexRetryTimeoutMs} is greater than
     *                                  {@code indexRetryMaxMs}
     */
    public DefaultIndexRetryHandler(EventTupleIndexingRepository<DiscoSolrDocument> repository,
                                    IndexDTOMapper dtoMapper) {
        this(repository, dtoMapper,100, 120000);
    }

    /**
     * Retry indexing operations every {@code (indexRetryMs * indexRetryBackoffFactor)} ms, up to
     * {@code indexRetryMaxMs}.
     * Constructs a retry handler with:
     * <dl>
     *     <dt>indexRetryBackoffFactor</dt>
     *     <dd>1.5</dd>
     * </dl>
     *
     * @param repository the Solr repository, must not be {@code null}
     * @param dtoMapper maps {@code IndexDTO} objects to a stream of {@code EventDiscoTuple}
     * @param indexRetryTimeoutMs initial time to wait between retry attempts, in ms
     * @param indexRetryMaxMs  absolute amount of time to wait before timing out, in ms
     * @throws IllegalArgumentException if {@code repository} is {@code null}, if any time-out related parameter is not
     *                                  1 or greater, if {@code indexRetryTimeoutMs} is greater than
     *                                  {@code indexRetryMaxMs}
     */
     public DefaultIndexRetryHandler(EventTupleIndexingRepository<DiscoSolrDocument> repository,
                                     IndexDTOMapper dtoMapper, int indexRetryTimeoutMs, int indexRetryMaxMs) {
        this(repository, dtoMapper, indexRetryTimeoutMs, indexRetryMaxMs, 1.5F);
     }

    /**
     * Retry indexing operations every {@code (indexRetryMs * indexRetryBackoffFactor)} ms, up to
     * {@code indexRetryMaxMs}.
     *
     * @param repository the Solr repository, must not be {@code null}
     * @param dtoMapper maps {@code IndexDTO} objects to a stream of {@code EventDiscoTuple}
     * @param indexRetryTimeoutMs initial time to wait between retry attempts, in ms
     * @param indexRetryMaxMs  absolute amount of time to wait before timing out, in ms
     * @param indexRetryBackoffFactor multiplied by the {@code indexRetryTimeoutMs} on each attempt
     * @throws IllegalArgumentException if {@code repository} is {@code null}, if any time-out related parameter is not
     *                                  1 or greater, if {@code indexRetryTimeoutMs} is greater than
     *                                  {@code indexRetryMaxMs}
     */
    public DefaultIndexRetryHandler(EventTupleIndexingRepository<DiscoSolrDocument> repository,
                                    IndexDTOMapper dtoMapper, int indexRetryTimeoutMs, int indexRetryMaxMs,
                                    float indexRetryBackoffFactor) {
        this.indexRetryTimeoutMs = assertPositive(indexRetryTimeoutMs,
                iae("Index retry timeout must be a positive integer."));
        this.indexRetryBackoffFactor = assertPositive(indexRetryBackoffFactor,
                iae("Index retry backoff factor must be a positive float greater than one."));
        this.indexRetryMaxMs = assertPositive(indexRetryMaxMs,
                iae("Index retry max ms must be a positive integer."));
        this.repository = assertNotNull(repository, iae("Repository must not be null."));
        this.dtoMapper = assertNotNull(dtoMapper, iae("DTO Mapper must not be null."));

        validateRetryMaxMs(indexRetryTimeoutMs, indexRetryMaxMs,
                "Index retry max ms must be equal or greater than index retry timeout");
    }

    /**
     * Attempts to index the supplied {@code dto} until the operation succeeds, or exceeds
     * {@link #getIndexRetryMaxMs()}.
     *
     * @param dto the data transfer object to index
     * @throws IndexingTimeoutException if {@code indexRetryMaxMs} is exceeded prior to successfully indexing the
     *                                  {@code dto}
     * @throws IndexingInterruptedException if the thread performing the indexing is interrupted before successfully
     *                                      indexing the {@code dto}
     */
    @Override
    public void retry(IndexDTO dto, Consumer<DiscoSolrDocument> documentDecorator)
            throws IndexingTimeoutException, IndexingInterruptedException {
        long start = currentTimeMillis();
        int attempt = 1;
        long timeout = indexRetryTimeoutMs;
        boolean success = false;
        Exception retryFailure = null;

        do {

            try {
                LOG.debug("Retry attempt {} (total elapsed time {} ms) indexing {}",
                        attempt, (currentTimeMillis() - start), dto);
                repository.index(dtoMapper.apply(dto), documentDecorator);
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
                    String fmt = "Retry operation was interrupted after %s attempts, %s ms: failed to index %s";
                    String msg = String.format(fmt, attempt, (currentTimeMillis() - start), dto);
                    LOG.error(msg);
                    throw new IndexingInterruptedException(msg, e);
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

    /**
     * The amount of time in ms, combined with {@link #getIndexRetryBackoffFactor() the backoff factor}, determines
     * how long to wait between retry attempts.  For example, if the {@code indexRetryTimeoutMs} is {@code 100}, and the
     * {@code indexRetryBackoffFactor} is {@code 1.5}, the first retry attempt will occur after 100 ms, the second
     * retry attempt in (100*1.5) 150 ms, and the third retry attempt in (150*1.5) 225 ms, and so on, until
     * {@code indexRetryMaxMs} is exceeded.
     *
     * @return initial time to wait, in ms, between retry attempts (combined with {@code indexRetryBackoffFactor}), must
     *         be a positive integer and less than {@code indexRetryMaxMs}
     */
    public int getIndexRetryTimeoutMs() {
        return indexRetryTimeoutMs;
    }

    /**
     * The amount of time in ms, combined with {@link #getIndexRetryBackoffFactor() the backoff factor}, determines
     * how long to wait between retry attempts.  For example, if the {@code indexRetryTimeoutMs} is {@code 100}, and the
     * {@code indexRetryBackoffFactor} is {@code 1.5}, the first retry attempt will occur after 100 ms, the second
     * retry attempt in (100*1.5) 150 ms, and the third retry attempt in (150*1.5) 225 ms, and so on, until
     * {@code indexRetryMaxMs} is exceeded.
     *
     * @param indexRetryTimeoutMs initial time to wait, in ms, between retry attempts (combined with
     *                            {@code indexRetryBackoffFactor}), must be a positive integer and less than
     *                            {@code indexRetryMaxMs}
     */
    public void setIndexRetryTimeoutMs(int indexRetryTimeoutMs) {
        validateRetryMaxMs(indexRetryTimeoutMs, indexRetryMaxMs,
                "Retry max ms must be a positive integer and be greater than the retry timeout.");
        this.indexRetryTimeoutMs = assertPositive(indexRetryTimeoutMs,
                iae("Index retry timeout must be a positive integer."));
    }

    /**
     * A multiplier that determines the amount of time to wait between retry attempts.  For example, if the
     * {@code indexRetryTimeoutMs} is {@code 100}, and the {@code indexRetryBackoffFactor} is {@code 1.5}, the first
     * retry attempt will occur after 100 ms, the second retry attempt in (100*1.5) 150 ms, and the third retry attempt
     * in (150*1.5) 225 ms, and so on, until {@code indexRetryMaxMs} is exceeded.
     *
     * @return the multiplier which determines the amount of time to wait between retry attempts (combined with
     *         {@code indexRetryTimeoutMs}), must be a positive value greater than or equal to 1.
     */
    public float getIndexRetryBackoffFactor() {
        return indexRetryBackoffFactor;
    }

    /**
     * A multiplier that determines the amount of time to wait between retry attempts.  For example, if the
     * {@code indexRetryTimeoutMs} is {@code 100}, and the {@code indexRetryBackoffFactor} is {@code 1.5}, the first
     * retry attempt will occur after 100 ms, the second retry attempt in (100*1.5) 150 ms, and the third retry attempt
     * in (150*1.5) 225 ms, and so on, until {@code indexRetryMaxMs} is exceeded.
     *
     * @param indexRetryBackoffFactor the multiplier which determines the amount of time to wait between retry attempts
     *                                (combined with {@code indexRetryTimeoutMs}), must be a positive value greater than
     *                                or equal to 1.
     */
    public void setIndexRetryBackoffFactor(float indexRetryBackoffFactor) {
        this.indexRetryBackoffFactor = assertPositive(indexRetryTimeoutMs,
                iae("Index retry backoff factor must be a positive float greater than or equal to one."));
    }

    /**
     * The absolute amount of time (over all retry attempts) to wait for a successful indexing operation.
     *
     * @return the maximum amount of time to wait over all indexing attempts, in ms
     */
    public int getIndexRetryMaxMs() {
        return indexRetryMaxMs;
    }

    /**
     * The absolute amount of time (over all retry attempts) to wait for a successful indexing operation.
     *
     * @param indexRetryMaxMs the maximum amount of time to wait over all indexing attempts, in ms
     */
    public void setIndexRetryMaxMs(int indexRetryMaxMs) {
        validateRetryMaxMs(1, indexRetryMaxMs,
                "Retry max ms must be a positive integer and be greater than the retry timeout.");
        this.indexRetryMaxMs = assertPositive(indexRetryMaxMs,
                iae("Index retry max ms must be a positive integer."));
    }

    public EventTupleIndexingRepository<DiscoSolrDocument> getRepository() {
        return repository;
    }

    public void setRepository(EventTupleIndexingRepository<DiscoSolrDocument> repository) {
        this.repository = repository;
    }

    public IndexDTOMapper getDtoMapper() {
        return dtoMapper;
    }

    public void setDtoMapper(IndexDTOMapper dtoMapper) {
        this.dtoMapper = dtoMapper;
    }

    private static void validateRetryMaxMs(int indexRetryTimeoutMs, int indexRetryMaxMs, String s) {
        if (indexRetryMaxMs < indexRetryTimeoutMs) {
            throw new IllegalArgumentException(s);
        }
    }

}
