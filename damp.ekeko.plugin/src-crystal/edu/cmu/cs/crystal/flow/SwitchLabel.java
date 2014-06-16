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

import org.eclipse.jdt.core.dom.Expression;


/**
 * A switch label occurs from the switch control flow.
 * This label maintains a link to the expression which it matched for.
 * @author ciera
 *
 */
public class SwitchLabel implements ILabel {
	private Expression matchExpression;
	
	public SwitchLabel(Expression matchExpression) {
		super();
		this.matchExpression = matchExpression;
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
				+ ((matchExpression == null) ? 0 : matchExpression.hashCode());
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
		final SwitchLabel other = (SwitchLabel) obj;
		if (matchExpression == null) {
			if (other.matchExpression != null)
				return false;
		} else if (!matchExpression.equals(other.matchExpression))
			return false;
		return true;
	}

	/**
	 * @return the expression which this case is matching on (not the expression it switched on)
	 */
	public Expression getMatchExpression() {
		return matchExpression;
	}

	public void setMatchExpression(Expression matchExpression) {
		this.matchExpression = matchExpression;
	}

	/**
	 * @return a string representation of the matching case expression, or "default" for the default case.
	 */
	public String getLabel() {
		if (matchExpression == null)
			return "default";
		else
			return matchExpression.toString();
	}

}
