package rw.coopconnect.exception;

/**
 * Thrown when a requested entity cannot be found.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String entityName, Long id) {
        super(entityName + " not found with id: " + id);
    }
}
