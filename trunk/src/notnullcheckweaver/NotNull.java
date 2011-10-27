package notnullcheckweaver;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

/**
 * Indicates that null is not a valid value for a parameter, method result, or field.
 * Placing this annotation on a class or package implicitly applies it to all elements within
 * the class or package, including subpackages.
 * 
 * <p>For @NotNull fields, the not-null check weaver checks
 * at field assignments that the value being assigned is not null,
 * at field reads that the field already has a non-null value,
 * and at the end of each constructor (or static initializer for static fields) that the field has been initialized.
 * Note that only accesses within the declaring class are checked.</p>     
 */
@Documented
@Target({ElementType.PACKAGE, ElementType.TYPE, ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface NotNull {
}
