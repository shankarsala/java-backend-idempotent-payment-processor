package in.shankar.sala.paymentprocessor.exception;

public class WalletNotFoundException extends RuntimeException {

    public WalletNotFoundException(String message) {
        super(message);
    }
}
