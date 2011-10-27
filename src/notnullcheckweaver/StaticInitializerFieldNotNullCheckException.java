package notnullcheckweaver;

/**
 * Thrown at the end of a static initializer if a @NotNull static field was not initialized. 
 */
public class StaticInitializerFieldNotNullCheckException extends NotNullCheckException {
	private static final long serialVersionUID = 1L;
	
	private final String fieldName;
	
	public StaticInitializerFieldNotNullCheckException(String fieldName) {
		super("Static initializer did not initialize @NotNull field "+fieldName+".");
		this.fieldName = fieldName;
	}
	
	public String getFieldName() {
		return fieldName;
	}
}
