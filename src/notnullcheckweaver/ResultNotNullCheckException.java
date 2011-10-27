package notnullcheckweaver;

/**
 * Thrown when the result of a @NotNull method is null.
 */
public class ResultNotNullCheckException extends NotNullCheckException {
	private static final long serialVersionUID = 1L;

	public ResultNotNullCheckException() {
		super("Returning null from a @NotNull method.");
	}
}
