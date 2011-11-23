package test;

import notnullcheckweaver.ArgumentNotNullCheckException;
import notnullcheckweaver.NotNull;
import notnullcheckweaver.Nullable;
import notnullcheckweaver.ResultNotNullCheckException;

import org.junit.Assert;
import org.junit.Test;

public class BasicTest {

	int foo(BasicTest o) {
		return 10;
	}
	
	@Test
	public void singleObjectArgument() {
		Assert.assertEquals(10, foo(this));
		
		try {
			foo(null);
			Assert.fail();
		} catch (ArgumentNotNullCheckException e) {
			Assert.assertEquals(e.getArgumentIndex(), 0);
		}
	}
	
	static long bar(int x, long y, Object o, double z, float t, @Nullable BasicTest xx, String xxx) {
		return 100;
	}
	
	@Test
	public void manyArguments() {
		Assert.assertEquals(100, bar(1, 2, this, 3, 4, this, "Hi"));
		
		try {
			bar(2, 3, null, 4, 5, this, "foo");
			Assert.fail();
		} catch (ArgumentNotNullCheckException e) {
			Assert.assertEquals(2, e.getArgumentIndex());
		}
		
		bar(2, 3, this, 4, 5, null, "foo");

		try {
			bar(2, 3, this, 4, 5, this, null);
			Assert.fail();
		} catch (ArgumentNotNullCheckException e) {
			Assert.assertEquals(6, e.getArgumentIndex());
		}
	}
	
	@Test
	public void nullableClass() {
		@Nullable
		class Foo {
			void bar(Object object) {
			}
			
			void baz(@NotNull Object quux) {
			}
		}
		
		new Foo().bar(null);
		
		try {
			new Foo().baz(null);
			Assert.fail();
		} catch (ArgumentNotNullCheckException e) {
			Assert.assertEquals(0, e.getArgumentIndex());
		}
	}
	
	Object baz(@Nullable Object x) {
		return x;
	}
	
	String quux(int x, @Nullable String y) {
		if (x < 0) {
			return "Hi";
		} else {
			return y;
		}
	}
	
	@Test
	public void result() {
		Object o = new Object();
		Assert.assertEquals(o, baz(o));
		
		try {
			baz(null);
			Assert.fail();
		} catch (ResultNotNullCheckException e) {
		}
		
		Assert.assertEquals("Hi", quux(-10, null));
		Assert.assertEquals("Bye", quux(10, "Bye"));
		
		try {
			quux(10, null);
		} catch (ResultNotNullCheckException e) {
		}
	}
	
	@Test
	public void subpackageTest() {
		test.sub.SubpackageTest.subpackageTest();
	}
}
