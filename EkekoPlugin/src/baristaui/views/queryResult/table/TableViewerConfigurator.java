package baristaui.views.queryResult.table;

import java.util.List;
import java.util.Map;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.TableColumn;

import barista.IResults;

public class TableViewerConfigurator {
	
	private TableViewer viewer;

	
	
	@SuppressWarnings("unchecked")
	public void configureFor(Map<String,List<Object>> results, String[] variables){
		for (int i = 0; i < variables.length; i++) {
			TableViewerColumn column = createTableViewerColumn(variables[i], 100, i);
			column.setLabelProvider(new SOULTableLabelProvider(variables[i]));
		}
		
		getViewer().setContentProvider(new IResultContentProvider(results,variables));
		getViewer().setInput(results);
		
	}

	
	private TableViewerColumn createTableViewerColumn(String title, int bound,
			final int colNumber) {
		final TableViewerColumn viewerColumn = new TableViewerColumn(getViewer(),
				SWT.NONE);
		final TableColumn column = viewerColumn.getColumn();
		column.setText(title);
		column.setWidth(bound);
		column.setResizable(true);
		column.setMoveable(true);
		return viewerColumn;

	}


	public void setViewer(TableViewer viewer) {
		this.viewer = viewer;
	}


	public TableViewer getViewer() {
		return viewer;
	}
}
