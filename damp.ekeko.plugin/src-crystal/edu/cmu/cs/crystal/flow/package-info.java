/* Copyright (c) 2006-2009 Marwan Abi-Antoun, Jonathan Aldrich, Nels E. Beckman,    
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

/**
 * This package is used for flow analyses. There are five type hierarchies to notice here:
 * <ul>
 * <li>IFlowAnalysisDefinition, and it's subtypes, are the transfer functions of a flow analysis. Every flow
 * analysis will need to implement transfer functions.
 * <li>ILatticeOperations are the operations on a lattice. Every flow analysis will need to implement this.
 * <li>ILabel, and its subtypes, are only used for branch-sensitive flow analyses.
 * <li>IResult, and its subtypes, are also for branch-sensitive flow analyses.
 * <li>IFlowAnalysis, and its subtypes, will run the worklist algorithm. Unless you are creating
 * a new category of flow analyses, you do not need to extend from this hierarchy, but you will need to instantiate one
 * of these within an ICrystalAnalysis.
 * </ul>
 */
package edu.cmu.cs.crystal.flow;