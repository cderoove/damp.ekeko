/**
 * Copyright (c) 2006, 2007, 2008 Marwan Abi-Antoun, Jonathan Aldrich, Nels E. Beckman,
 * Kevin Bierhoff, David Dickey, Ciera Jaspan, Thomas LaToza, Gabriel Zenarosa, and others.
 *
 * This file is part of Crystal.
 *
 * Crystal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Crystal is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Crystal.  If not, see <http://www.gnu.org/licenses/>.
 */
package edu.cmu.cs.crystal.flow;

import org.eclipse.jdt.core.dom.ITypeBinding;


/**
 * A label for edges that are on exceptional control flow. This maintains
 * a link to the ITypeBinding of the exception for this label.
 * This occurs in exceptional control flow from throws, method calls,etc.
 * @author ciera
 *
 */
public class ExceptionalLabel implements ILabel {
	private ITypeBinding exceptionType;

	public ExceptionalLabel(ITypeBinding exceptionType) {
		this.exceptionType = exceptionType;
	}
	
	/**
	 * 
	 * @return the type of the exception that is thrown on this control flow
	 */
	public ITypeBinding getExceptionType() {
		return exceptionType;
	}

	public void setExceptionType(ITypeBinding branchValue) {
		this.exceptionType = branchValue;
	}
	
	/**
	 * @return the fully qualified name for the exception type
	 */
	public String getLabel() {
	
		return exceptionType.getQualifiedName();
	}

	@Override
	public String toString() {
		return getLabel();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((exceptionType == null) ? 0 : exceptionType.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final ExceptionalLabel other = (ExceptionalLabel) obj;
		if (exceptionType == null) {
			if (other.exceptionType != null)
				return false;
		} else if (!exceptionType.equals(other.exceptionType))
			return false;
		return true;
	}
}
