package rw.coopconnect.exception;

/**
 * FR-06: Thrown when order quantity exceeds available stock.
 */
public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(String message) {
        super(message);
    }

    public InsufficientStockException(Long listingId, String requested, String available) {
        super("Insufficient stock for listing " + listingId +
              ": requested " + requested + ", available " + available);
    }
}
