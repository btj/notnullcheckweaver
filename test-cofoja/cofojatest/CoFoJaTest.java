// Tests compatibility of notnullcheckweaver with cofoja

package cofojatest;

import notnullcheckweaver.*;
import com.google.java.contract.*;
import org.junit.*;

class Foo {
    
    @Requires("arg.toString().equals(\"Hey\")")
    @Ensures("arg.toString().equals(\"Heyfrob\")")
    public Foo(StringBuilder arg) {
        arg.append("frob");
    }
    
}

public class CoFoJaTest {
    
    @Requires("arg.equals(\"Hi\")")
    @Ensures("result == 10")
    public static int foo(String arg) {
        return 10;
    }
    
    @Test
    public void validMethodCallTest() {
        foo("Hi");
    }
    
    @Test(expected=ArgumentNotNullCheckException.class)
    public void methodArgumentNotNullCheckExceptionTest() {
        foo(null);
    }
    
    @Test(expected=PreconditionError.class)
    public void methodPreconditionErrorTest() {
        foo("Bye");
    }
    
    @Test
    public void validCtorCallTest() {
        new Foo(new StringBuilder("Hey"));
    }
    
    @Test(expected=ArgumentNotNullCheckException.class)
    public void ctorArgumentNotNullCheckExceptionTest() {
        new Foo(null);
    }
    
    @Test(expected=PreconditionError.class)
    public void ctorPreconditionErrorTest() {
        new Foo(new StringBuilder("Bye"));
    }
    
}