package info.rmapproject.indexing;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class IndexingTimeoutException extends Exception {

    private static final long serialVersionUID = 1L;

    public IndexingTimeoutException(String message) {
        super(message);
    }

    public IndexingTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }

    public IndexingTimeoutException(Throwable cause) {
        super(cause);
    }

    public IndexingTimeoutException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
