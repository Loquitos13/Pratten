package beringela.software.common;

/** Raised when a request violates a domain rule (e.g. paying a cancelled order). */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
