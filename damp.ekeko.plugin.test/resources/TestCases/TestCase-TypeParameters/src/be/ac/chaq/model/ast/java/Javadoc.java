package be.ac.chaq.model.ast.java;

import java.util.List;

import be.ac.chaq.model.entity.EntityIdentifier;
import be.ac.chaq.model.entity.EntityListProperty;

public class Javadoc extends Comment {
	@EntityListProperty(value = TagElement.class)
	private List<EntityIdentifier> tags;

	public List<EntityIdentifier> getTags() {
		return tags;
	}

	public void setTags(List<EntityIdentifier> tags) {
		this.tags = tags;
	}
}