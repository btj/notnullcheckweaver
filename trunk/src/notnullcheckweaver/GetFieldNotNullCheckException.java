package notnullcheckweaver;

/**
 * Thrown when reading an uninitialized @NotNull field. 
 */
public class GetFieldNotNullCheckException extends NotNullCheckException {
	private static final long serialVersionUID = 1L;
	
	public GetFieldNotNullCheckException() {
		super("Attempt to read uninitialized @NotNull field.");
	}
}
