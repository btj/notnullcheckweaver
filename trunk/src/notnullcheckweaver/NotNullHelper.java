package notnullcheckweaver;

/**
 * Contains methods used by the generated code.   
 */
public final class NotNullHelper {
	private NotNullHelper() {}
	
	public static void checkArgumentNotNull(Object object, int index) {
		if (object == null)
			throw new ArgumentNotNullCheckException(index);
	}
	
	public static void checkResultNotNull(Object object) {
		if (object == null)
			throw new ResultNotNullCheckException();
	}
	
	public static void checkPutFieldNotNull(Object object) {
		if (object == null)
			throw new PutFieldNotNullCheckException();
	}
	
	public static void checkGetFieldNotNull(Object object) {
		if (object == null)
			throw new GetFieldNotNullCheckException();
	}
	
	public static void checkConstructorFieldNotNull(Object object, String fieldName) {
		if (object == null)
			throw new ConstructorFieldNotNullCheckException(fieldName);
	}
	
	public static void checkStaticInitializerFieldNotNull(Object object, String fieldName) {
		if (object == null)
			throw new StaticInitializerFieldNotNullCheckException(fieldName);
	}
}
