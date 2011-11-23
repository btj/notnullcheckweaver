package test.sub;

import notnullcheckweaver.NotNull;
import notnullcheckweaver.ArgumentNotNullCheckException;
import org.junit.Assert;

public class SubpackageTest {
	static void foo(Object o) {
	}
	
	static void bar(@NotNull Object o) {}
	
	public static void subpackageTest() {
		foo(null);
		try {
			bar(null);
			Assert.fail();
		} catch (ArgumentNotNullCheckException e) {
			Assert.assertEquals(0, e.getArgumentIndex());
		}
	}
}
