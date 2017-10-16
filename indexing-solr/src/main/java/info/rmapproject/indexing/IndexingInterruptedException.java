package info.rmapproject.indexing;

public class IndexingInterruptedException extends Exception {

    public IndexingInterruptedException(String message) {
        super(message);
    }

    public IndexingInterruptedException(String message, Throwable cause) {
        super(message, cause);
    }

    public IndexingInterruptedException(Throwable cause) {
        super(cause);
    }

    public IndexingInterruptedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
