package baristaui.views.queryResult.tree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

public class TreeResultContentProvider implements ITreeContentProvider {

	String[] variables;
	Map[] values;
	    
	public Map[] getValues() {
		return values;
	}


	public class Node{
		public Object from;
		int index;
		public Node(Object from, int index) {
			super();
			this.from = from;
			this.index = index;
		}
		
		@Override
		public int hashCode() {
			if(from != null)
				return from.hashCode();
			return super.hashCode();
		}
		
		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Node) {
				Node that = (Node) obj;
				return this.from.equals(that.from);
			}
			return super.equals(obj);
		}
		
		@Override
		public String toString() {
			return from +":" +index;
		}
	}
	    
	public TreeResultContentProvider(String[] variables) {
		this.variables = variables;
	}


	@Override
	public void dispose() {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public Object[] getElements(Object inputElement) {
		if (inputElement instanceof Map) {
			Map results = (Map) inputElement;
			createMaps(results);
			if(values.length == 0) return new Object[0];
			return values[0].keySet().toArray();
		}
		return null;
	}


	private void createMaps(Map results) {
		values = new Map[variables.length];
		for (int i = 0; i < variables.length; i++) {
			values[i] = new HashMap();
			List<Object> bindings = (List<Object>) results.get(variables[i]);
			int j = 0;
			for (Object binding : bindings) {
				Node key = new Node(binding,i); 
				if(values[i].get(key)==null){
					values[i].put(key, new HashSet<Object>());
				}
				Object to = null;
				if(i<variables.length-1) to = ((List<Object>)results.get(variables[i+1])).get(j++);
				
				((Set<Object>)values[i].get(key)).add(new Node(to,i+1));				
			}
			
		}
		
		
	}


	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof Node) {
			Node node = (Node)parentElement;
			return ((Set<Object>)values[node.index].get(node)).toArray();
			                     
		}
		return null;
	}


	@Override
	public Object getParent(Object element) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public boolean hasChildren(Object element) {
		if (element instanceof Node) {
			Node node = (Node)element;
			return node.index < variables.length-1;
		}
		return false;
	}

	

}
