package baristaui.views.queryResult.variableTable;

import java.util.List;
import java.util.Map;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.TableViewer;

public class VariableTableConfigurator {
	
	private TableViewer viewer;

	public VariableTableConfigurator(TableViewer variableTable) {
		this.viewer = variableTable;
	}

	public void configureFor(Map<String,List<Object>> results, String[] variables) {
		viewer.setContentProvider(new ArrayContentProvider());
		viewer.setInput(variables);
	
	
	}

}
