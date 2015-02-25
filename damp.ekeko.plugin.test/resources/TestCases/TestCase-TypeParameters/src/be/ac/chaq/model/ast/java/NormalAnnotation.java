package be.ac.chaq.model.ast.java;

import java.util.List;

import be.ac.chaq.model.entity.EntityIdentifier;
import be.ac.chaq.model.entity.EntityListProperty;

public class NormalAnnotation extends Annotation {
	@EntityListProperty(value = MemberValuePair.class)
	private List<EntityIdentifier> values;

	public List<EntityIdentifier> getValues() {
		return values;
	}

	public void setValues(List<EntityIdentifier> values) {
		this.values = values;
	}
}