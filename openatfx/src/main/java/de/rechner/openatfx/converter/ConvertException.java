package de.rechner.openatfx.converter;

/**
 * Exception thrown by the DAT2AFTX converter.
 * 
 * @author Christian Rechner
 */
public class ConvertException extends Exception {

    private static final long serialVersionUID = -800172930309573117L;

    /**
     * Creates a new exception having a message and a nested exception.
     * 
     * @param message The message.
     * @param cause The nested exception.
     */
    public ConvertException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new exception having a message.
     * 
     * @param message The exception message.
     */
    public ConvertException(String message) {
        super(message);
    }

}
