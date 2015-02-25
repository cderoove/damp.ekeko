package be.ac.chaq.model.entity;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import be.ac.chaq.model.ast.java.IDocElement;

@Target(value = ElementType.FIELD)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface EntityListProperty {
	String name() default "";
	//cannot be "? extends EntityState" because of interface types such as IDocElement that are used within the Java AST hierarchy
	Class<?> value() default EntityState.class; 
}
