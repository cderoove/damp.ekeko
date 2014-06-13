package baristaui.views.queryResult.tree;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;


public class IResultTreeContentProvider implements ITreeContentProvider {

	private String[] variables;
	
	
	class Node{
		Node parent;
		String key;
		Object binding;
		List<Node> children; 
	}
	
	public IResultTreeContentProvider(String[] variables) {
		this.variables = variables;
	}



	@Override
	public Object[] getElements(Object inputElement) {
		if (inputElement instanceof Map) {
			Map results = (Map) inputElement;
			List<Node> roots = createTree(results);
			return (Object[]) roots.toArray(new Object[roots.size()]);
		}
		return null;
	}



	private List<Node> createTree(Map results) {
		if(variables.length==0) return new ArrayList<IResultTreeContentProvider.Node>();
		List<Object> objs = (List<Object>) results.get(variables[0]);
		List<Node> children = new ArrayList<IResultTreeContentProvider.Node>(objs.size());
		Object prev = null;
		Node node = null;
		int curr = 0;
		for (Object object : objs) {
			ArrayList<Node> newChildren = new ArrayList<Node>();
			if(!object.equals(prev)){
				node = new Node();
				node.key = variables[0];
				node.children = new ArrayList<Node>();
				children.add(node);
				prev = object;
			}
			node.binding = prev;
			createTree(results,1,curr++,node,newChildren);
			node.children.addAll(newChildren);
		}
		return children;	
	}



	private int createTree(Map results, int varIndex, int currBinding, Node parent, List<Node> newChildren) {
		if(varIndex == variables.length)
			return currBinding; //XXX should return num 
		String key = variables[varIndex];
		List<Object> objs = (List<Object>) results.get(key);
		varIndex++;
		Object prevBinding = objs.get(currBinding);
		Node node = new Node();
		node.parent = parent;
		node.key  = key;
		node.children = new ArrayList<Node>();
		newChildren.add(node);
		
		for (int j = currBinding; j<objs.size();j++) {
			Object object = objs.get(j);
			if(!object.equals(prevBinding)){
				return j;
			}
			node.binding = object;	
			List<Node> children = new ArrayList<IResultTreeContentProvider.Node>();
			j = createTree(results,varIndex,currBinding,node,children);
			node.children.addAll(children);
			
		}
		return objs.size();
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
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof Node) {
			Node node = (Node) parentElement;
			return node.children.toArray();
		}
		return null;
	}



	@Override
	public Object getParent(Object element) {
		if (element instanceof Node) {
			Node node = (Node) element;
			return node.parent;
		}
		return null;
	}



	@Override
	public boolean hasChildren(Object element) {
		if (element instanceof Node) {
			Node node = (Node) element;
			return !node.children.isEmpty();
		}
		return false;
	}

}
