package co.rsk.bitcoinj.core;

public class SignatureDecodeException extends Exception {
    public SignatureDecodeException() {
        super();
    }

    public SignatureDecodeException(String message) {
        super(message);
    }

    public SignatureDecodeException(Throwable cause) {
        super(cause);
    }

    public SignatureDecodeException(String message, Throwable cause) {
        super(message, cause);
    }
}
