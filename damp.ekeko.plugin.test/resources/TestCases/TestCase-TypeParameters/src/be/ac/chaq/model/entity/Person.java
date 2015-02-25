package be.ac.chaq.model.entity;

import java.util.HashSet;
import java.util.Set;

public class Person {
	
	private PersonIdentifier id;
	
	private String name;
	
	private Set<String> email_addresses;

	public Person() {
		email_addresses = new HashSet<String>();
	}
	public PersonIdentifier getId() {
		return id;
	}

	public void setId(PersonIdentifier id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Set<String> getEmailAddresses() {
		return email_addresses;
	}
	
	public boolean addEmailAddress(String email) {
		return email_addresses.add(email);
	}
	
}
