package rw.coopconnect.exception;

/**
 * NFR-01: Thrown on unauthorized access attempts.
 */
public class UnauthorizedAccessException extends RuntimeException {

    public UnauthorizedAccessException(String message) {
        super(message);
    }
}
