package beringela.software.common;

/** Lançada quando uma conta excede as tentativas de login permitidas. */
public class TooManyAttemptsException extends RuntimeException {

    public TooManyAttemptsException(String message) {
        super(message);
    }
}
