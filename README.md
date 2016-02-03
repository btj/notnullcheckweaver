This project delivers a Java agent (i.e. a bytecode transformer that is executed on the fly by the JVM as it loads classes) that adds run-time checks that enforce `@NotNull` annotations on parameters, method results, and fields.

# Motivation #

The programming style where the programmer inserts thorough validity checks of parameter values at the top of each method (or at least those that constitute the exported interface of the module) is a very beneficial one. It helps reduce the distance between the source location where an error manifests itself and where it originates. Also, it helps the explicit separation of responsibilities and blame assignment between the modules of the system.

Unfortunately, due to the fact that every value of reference type in Java can be null, adhering to this style means inserting a large number of null checks throughout the program. For example:

```java
public class MyClass {

    /** Frobs the specified foo, bar, and baz.
      *
      * o1 and o2 should not be null.
      */
    public void frob(Foo o1, Bar o2, Baz o3) {
        if (o1 == null) throw new IllegalArgumentException("o1 is null");
        if (o2 == null) throw new IllegalArgumentException("o2 is null");
        // do the actual frobbing
    }

}
```

By using `notnullcheckweaver`, you can reduce this code to the following:

```java
@NotNull
public class MyClass {

    /** Frobs the specified foo, bar, and baz. */
    public void frob(Foo o1, Bar o2, @Nullable Baz o3) {
        // do the actual frobbing
    }

}
```

Besides reducing boilerplate, this approach appropriately draws attention to nullable values, which deserve special attention, rather than the harmless not-null values.

To achieve this benefit, it would be sufficient if `notnullcheckweaver` checked just argument values. In fact, it checks also method results and field values. This yields the added benefit that also for these values one can assume, unless stated otherwise explicitly, that these values are never null.

# Basic walkthrough #

Here is an example program that uses `@NotNull`:

```java
package mypackage;

import notnullcheckweaver.NotNull;
import notnullcheckweaver.ArgumentNotNullCheckException;

@NotNull
public class MyProgram {

    private static void foo(Object object) {}
    
    public static void main(String[] args) {
        try {
            foo(null);
            System.out.println("It doesn't work :-(");
        } catch (ArgumentNotNullCheckException e) {
            System.out.println("It works! :-)");
        }
    }

}
```

Compile this program as follows:

```
javac -classpath notnullcheckweaver.jar mypackage/MyProgram.java
```

Run it as follows:

```
java -javaagent:notnullcheckweaver.jar mypackage.MyProgram
```

You should get the following output:

```
It works! :-)
```

# Further info #

A variable (parameter, method result, or field) is considered `@NotNull` either if it is marked as `@NotNull` directly, or if an enclosing element (class or package) is marked as `@NotNull` and no closer enclosing element is marked as `@Nullable`. For the purposes of this definition, package `foo` is considered to enclose package `foo.bar`.

The most convenient approach is to mark your toplevel package as `@NotNull`, and to put `@Nullable` on the individual parameters, method results, and fields for which `null` is a valid value.

For example:

```java
// mypackage/package-info.java

@NotNull
package mypackage;

import notnullcheckweaver.NotNull;
```

```java
// mypackage/mysubpackage/MyClass.java

package mypackage.mysubpackage;

import notnullcheckweaver.Nullable;

public class MyClass {

    public static void myMethod(MyClass foo, @Nullable MyClass bar) {
        // ...
    }

}
```

In the above example, due to the `@NotNull` annotation on package `mypackage`, parameter `foo` of method `myMethod` is considered `@NotNull` and the weaver will insert a not-null check for this parameter at the top of the bytecode of `myMethod` when `MyClass` is loaded.

See also the documentation in the distribution, and the test cases in the source repository.
