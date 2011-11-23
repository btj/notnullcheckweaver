package test;

import notnullcheckweaver.ArgumentNotNullCheckException;
import notnullcheckweaver.ConstructorFieldNotNullCheckException;
import notnullcheckweaver.GetFieldNotNullCheckException;
import notnullcheckweaver.Nullable;
import notnullcheckweaver.PutFieldNotNullCheckException;
import notnullcheckweaver.StaticInitializerFieldNotNullCheckException;

import org.junit.Assert;
import org.junit.Test;

public class FieldsTest {
	
	@Test
	public void fieldsTest() {
		class Foo {
			int dummy;
			String text;
			long dummy2;
			@Nullable String text2;
			
			Foo() {
			}
			
			Foo(int x) {
				dummy = x;
				System.out.println(text);
				dummy2 = x;
			}
			
			Foo(String t) {
				this.text = t;
			}
			
			Foo(@Nullable String t, int x) {
				dummy = x;
				this.text = t;
				dummy2 = x;
			}
			
			String getText() {
				return text;
			}
			
			void setText(@Nullable String s) {
				this.text = s;
			}
			
			@Nullable String getText2() {
				return text2;
			}
			
			void setText2(@Nullable String s) {
				text2 = s;
			}
		}
		
		Foo foo = new Foo("Hi");
		Assert.assertEquals("Hi", foo.getText());
		Assert.assertEquals("Hi", foo.text);
		Assert.assertEquals(0, foo.dummy);
		Assert.assertEquals(0, foo.dummy2);
		
		foo.setText("Bye");
		Assert.assertEquals("Bye", foo.getText());
		Assert.assertEquals("Bye", foo.text);
		
		try {
			foo = new Foo(null);
			Assert.fail();
		} catch (ArgumentNotNullCheckException e) {
			Assert.assertEquals(1, e.getArgumentIndex()); // Foo is an inner class; the outer FieldsTest object is passed implicitly as argument 0.
		}
		
		foo = new Foo("Hi", 42);
		Assert.assertEquals("Hi", foo.getText());
		Assert.assertEquals("Hi", foo.text);
		Assert.assertEquals(42, foo.dummy);
		Assert.assertEquals(42, foo.dummy2);
		foo.setText2(null);
		Assert.assertEquals(null, foo.getText2());
		
		try {
			foo.setText(null);
			Assert.fail();
		} catch (PutFieldNotNullCheckException e) {
		}
		
		try {
			new Foo(null, 42);
			Assert.fail();
		} catch (PutFieldNotNullCheckException e) {
		}
		
		try {
			new Foo();
			Assert.fail();
		} catch (ConstructorFieldNotNullCheckException e) {
			Assert.assertEquals("text", e.getFieldName());
		}
		
		try {
			new Foo(42);
			Assert.fail();
		} catch (GetFieldNotNullCheckException e) {
		}
	}
	
	@Test
	public void staticFields() {
		Bar.foo();
		
		try {
			Baz.foo();
		} catch (ExceptionInInitializerError e) {
			Assert.assertTrue(e.getException() instanceof StaticInitializerFieldNotNullCheckException);
			StaticInitializerFieldNotNullCheckException ee = (StaticInitializerFieldNotNullCheckException)e.getException();
			Assert.assertEquals("baz", ee.getFieldName());
		}
		
		Bazz.foo();
	}
	
	@Test
	public void anonClassInstance() {
		final Object foo = "Hi";
		new Object() {
			public String toString() {
				return (String)foo;
			}
		};
	}
}

class Bar {
	static int x;
	static String y = "1";
	static long z;
	static Object t = "2";
	static Bar b = new Bar();
	
	static void foo() {
		x = 3;
		Assert.assertEquals(3, x);
		y = "Hi";
		Assert.assertEquals("Hi", y);
		z = 4;
		Assert.assertEquals(4, z);
		t = "Foo";
		Assert.assertEquals("Foo", t);
		Bar bar = new Bar();
		b = bar;
		Assert.assertEquals(bar, b);
		
		try {
			t = null;
			Assert.fail();
		} catch (PutFieldNotNullCheckException e) {
		}
	}
}

class Baz {
	static Object baz;
	
	static void foo() {
	}
}

interface Quux {
	public static final int x = 10;
	public static final Object o = "Hi";
}

abstract class Bazz implements Quux {
	static Object baz;
	@Nullable static Object bazz;
	
	static {
		try {
			System.out.println(baz);
			Assert.fail();
		} catch (GetFieldNotNullCheckException e) {
		}
		
		baz = "Hey";
	}
	
	static void foo() {
		bazz = null;
		Assert.assertEquals(null, bazz);
	}
	
	abstract void bar();
}

enum FooBar { FOO, BAR }
