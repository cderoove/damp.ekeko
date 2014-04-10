package baristaui.views.queryResult.tree;

import java.util.Map;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import baristaui.views.queryResult.SOULLabelProvider;
import baristaui.views.queryResult.tree.TreeResultContentProvider.Node;


public class SOULTreeLabelProvider extends LabelProvider {

	private SOULLabelProvider provider;
	
	public SOULTreeLabelProvider(SOULLabelProvider p)  {
		 super();
		 provider = p;
	}
	
	@Override
	public String getText(Object element) {
		
		if (element instanceof Node) {
			
			Node node = (Node) element;
			return provider.getText(node.from);
		}
		
		return super.getText(element);
	}
	
	
	@Override
	public Image getImage(Object element) {
		if (element instanceof Node) {
			Node node = (Node) element;
			return provider.getImage(node.from);
		}
		return super.getImage(element);
	}
	
}
