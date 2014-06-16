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


/**
 * A boolean label is a label which is either true or false. The true
 * and false labels can be retrieved with getBooleanLabel(boolean).
 * It occurs branches from boolean expressions.
 * @author ciera
 *
 */
public class BooleanLabel implements ILabel {
	static private BooleanLabel TRUE_LABEL = new BooleanLabel(true);
	static private BooleanLabel FALSE_LABEL = new BooleanLabel(false);

	private boolean branchValue;
	
	private BooleanLabel(boolean branchValue) {
		this.branchValue = branchValue;
	}
	
	/**
	 * @param labelValue
	 * @return the BooleanLabel for the boolean passed in
	 */
	static public BooleanLabel getBooleanLabel(boolean labelValue) {
		return labelValue ? TRUE_LABEL : FALSE_LABEL;
	}
	
	public String getLabel() {return Boolean.toString(branchValue);}

	public boolean getBranchValue() {
		return branchValue;
	}

	@Override
	public String toString() {
		return getLabel();
	}
}
