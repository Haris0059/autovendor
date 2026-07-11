package ba.autovendor.backend.common;

public class WooPluginException extends RuntimeException {

    /** Upstream HTTP status from the WordPress plugin; 0 when unknown (e.g. connectivity failure). */
    private final int upstreamStatus;

    public WooPluginException(String message) {
        this(message, 0);
    }

    public WooPluginException(String message, int upstreamStatus) {
        super(message);
        this.upstreamStatus = upstreamStatus;
    }

    public WooPluginException(String message, Throwable cause) {
        this(message, cause, 0);
    }

    public WooPluginException(String message, Throwable cause, int upstreamStatus) {
        super(message, cause);
        this.upstreamStatus = upstreamStatus;
    }

    public int getUpstreamStatus() {
        return upstreamStatus;
    }
}
