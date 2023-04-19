package C2G8_Project;

public class UnregisteredDispatcherDestination extends RuntimeException {

	private static final long serialVersionUID = 8479279107463345331L;

	public UnregisteredDispatcherDestination() {
	}

	public UnregisteredDispatcherDestination(String message) {
		super(message);
	}

	public UnregisteredDispatcherDestination(Throwable cause) {
		super(cause);
	}

	public UnregisteredDispatcherDestination(String message, Throwable cause) {
		super(message, cause);
	}

	public UnregisteredDispatcherDestination(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
