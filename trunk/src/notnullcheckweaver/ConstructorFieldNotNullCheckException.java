package notnullcheckweaver;

/**
 * Thrown at the end of a constructor if a @NotNull field was not initialized. 
 */
public class ConstructorFieldNotNullCheckException extends NotNullCheckException {
	private static final long serialVersionUID = 1L;
	
	private final String fieldName;
	
	public ConstructorFieldNotNullCheckException(String fieldName) {
		super("Constructor did not initialize @NotNull field "+fieldName+".");
		this.fieldName = fieldName;
	}
	
	public String getFieldName() {
		return fieldName;
	}
}
