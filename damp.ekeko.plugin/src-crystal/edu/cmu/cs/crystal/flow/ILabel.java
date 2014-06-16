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
 * A label is a named edge on a control flow graph. This is used for distinguishing between
 * multiple branches on the control flow, and to allow a branch-senstive analysis to track
 * lattices along branches seperately.
 * 
 * Labels will properly override Object#equals and Object#hashCode, and so can be compared to each other
 * and used as keys in a hash.
 * 
 * @see IResult
 * @see IBranchSensitiveTransferFunction
 * @see ITACBranchSensitiveTransferFunction
 * 
 * @author Kevin Bierhoff
 */
public interface ILabel {
	
	public String getLabel();
}
