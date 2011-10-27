package notnullcheckweaver;

/**
 * Thrown when a field assignment attempts to assign null to a @NotNull field. 
 */
public class PutFieldNotNullCheckException extends NotNullCheckException {
	private static final long serialVersionUID = 1L;
	
	public PutFieldNotNullCheckException() {
		super("Attempt to assign null to @NotNull field.");
	}
}
