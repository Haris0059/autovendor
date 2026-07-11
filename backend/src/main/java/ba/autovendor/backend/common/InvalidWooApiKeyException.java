package ba.autovendor.backend.common;

public class InvalidWooApiKeyException extends RuntimeException {

    public InvalidWooApiKeyException(String message) {
        super(message);
    }
}
