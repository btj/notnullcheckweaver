package notnullcheckweaver;

/**
 * Thrown when the argument for a @NotNull parameter is null.
 */
public class ArgumentNotNullCheckException extends NotNullCheckException {
	private static final long serialVersionUID = 1L;
	
	/** Zero-based; not counting the receiver. */
	private final int index;
	
	/**
	 * Thrown when the argument for a @NotNull parameter is null.
	 * @param argumentIndex 0 if the first argument is null, 1 if the second argument is null, etc.
	 */
	public ArgumentNotNullCheckException(int argumentIndex) {
		super("Argument for @NotNull parameter "+argumentIndex+" is null.");
		this.index = argumentIndex;
	}
	
	public int getArgumentIndex() {
		return index;
	}
}
