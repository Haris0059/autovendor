package ba.autovendor.backend.common;

public class InvalidOlxCredentialsException extends RuntimeException {

    public InvalidOlxCredentialsException(String message) {
        super(message);
    }
}
