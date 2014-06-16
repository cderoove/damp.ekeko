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
 * A label for control flow where the branching flow is due to an iterator, for example,
 * in an enhanced for loop. There are two kinds of Iterator labels; an "empty" label and a
 * "has item" label.
 * 
 * @author ciera
 * @since Crystal 3.4.0
 */
public class IteratorLabel implements ILabel {
	static private IteratorLabel EMPTY_LABEL = new IteratorLabel(true);
	static private IteratorLabel HAS_ITEM_LABEL = new IteratorLabel(false);

	private boolean isEmpty;
	
	private IteratorLabel(boolean isEmpty) {
		this.isEmpty = isEmpty;
	}
	
	/**
	 * @param isEmpty true if the iterator is empty, false if it has more items
	 * @return an IteratorLabel which represents the appropriate state
	 */
	static public IteratorLabel getItrLabel(boolean isEmpty) {
		return isEmpty ? EMPTY_LABEL : HAS_ITEM_LABEL;
	}
	
	public String getLabel() {
		return isEmpty ? "empty" : "has item";
	}

	/**
	 * 
	 * @return true if this is the empty label, and false if it is the hasItem label.
	 */
	public boolean isEmptyLabel() {
		return isEmpty;
	}

	@Override
	public String toString() {
		return getLabel();
	}
}
