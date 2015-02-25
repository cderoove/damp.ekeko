package be.ac.chaq.model.entity;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(value = ElementType.FIELD)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface EntityProperty {
	String name() default "";
	Class<? extends EntityState> value() default EntityState.class;
}
