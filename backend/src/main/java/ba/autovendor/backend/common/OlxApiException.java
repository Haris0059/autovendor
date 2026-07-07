package ba.autovendor.backend.common;

public class OlxApiException extends RuntimeException {

    public OlxApiException(String message) {
        super(message);
    }

    public OlxApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
