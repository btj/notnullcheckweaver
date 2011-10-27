package notnullcheckweaver;

/**
 * Base class for the exceptions thrown by the not null checks. 
 */
public abstract class NotNullCheckException extends RuntimeException {
	public NotNullCheckException(String message) {
		super(message);
	}

	private static final long serialVersionUID = 1L;
}
