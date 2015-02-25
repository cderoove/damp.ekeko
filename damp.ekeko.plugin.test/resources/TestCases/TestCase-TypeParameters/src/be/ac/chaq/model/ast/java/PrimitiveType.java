package be.ac.chaq.model.ast.java;

import be.ac.chaq.model.entity.SimpleProperty;

public class PrimitiveType extends Type {

	public static class Code {

	}

	@SimpleProperty(value = Code.class)
	private Code primitiveTypeCode;

	public Code getPrimitivetypecode() {
		return primitiveTypeCode;
	}

	public void setPrimitivetypecode(Code primitiveTypeCode) {
		this.primitiveTypeCode = primitiveTypeCode;
	}
}