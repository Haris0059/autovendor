package ba.autovendor.backend.common;

public class OlxApiException extends RuntimeException {

    /** Upstream HTTP status from OLX; 0 when unknown (e.g. connectivity failure). */
    private final int upstreamStatus;

    public OlxApiException(String message) {
        this(message, 0);
    }

    public OlxApiException(String message, int upstreamStatus) {
        super(message);
        this.upstreamStatus = upstreamStatus;
    }

    public OlxApiException(String message, Throwable cause) {
        this(message, cause, 0);
    }

    public OlxApiException(String message, Throwable cause, int upstreamStatus) {
        super(message, cause);
        this.upstreamStatus = upstreamStatus;
    }

    public int getUpstreamStatus() {
        return upstreamStatus;
    }
}
