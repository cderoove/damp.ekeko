package be.ac.chaq.model.ast.java;

import java.util.List;

import be.ac.chaq.model.entity.EntityIdentifier;
import be.ac.chaq.model.entity.EntityListProperty;
import be.ac.chaq.model.entity.SimpleProperty;

public class TagElement extends ASTNode implements IDocElement {
	@SimpleProperty(value = String.class)
	private String tagName;

	@EntityListProperty(value = IDocElement.class)
	private List<EntityIdentifier> fragments;

	public String getTagname() {
		return tagName;
	}

	public List<EntityIdentifier> getFragments() {
		return fragments;
	}

	public void setTagname(String tagName) {
		this.tagName = tagName;
	}

	public void setFragments(List<EntityIdentifier> fragments) {
		this.fragments = fragments;
	}
}