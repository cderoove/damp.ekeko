package baristaui.views.queryResult.tree;

import java.util.List;
import java.util.Map;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;

import barista.IResults;
import baristaui.views.queryResult.SOULLabelProvider;

public class TreeViewerConfigurator {

	private TreeViewer viewer;

	private LabelProvider provider;
	
	public TreeViewerConfigurator(LabelProvider provider) {
		super();
		this.provider = provider;
	}
	
	public void configureFor(Map<String, List<Object>> results, String[] variables){
		viewer.setContentProvider(new TreeResultContentProvider(variables));
		viewer.setLabelProvider(new SOULTreeLabelProvider(provider));
		
		viewer.setInput(results);
	}
	
	
	public void setViewer(TreeViewer viewer) {
		this.viewer = viewer;
	}

	public TreeViewer getViewer() {
		return viewer;
	}
	
}
