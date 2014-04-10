package baristaui.views.queryResult.column;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;

import baristaui.views.queryResult.tree.TreeResultContentProvider;
import baristaui.views.queryResult.tree.TreeResultContentProvider.Node;

public class ColumnSelectionListener implements IDoubleClickListener {

	TableViewer[] columns;
	int index;
	TreeResultContentProvider provider;
	
	
	
	
	public ColumnSelectionListener(TableViewer[] columns, int index,
			TreeResultContentProvider provider) {
		super();
		this.columns = columns;
		this.index = index;
		this.provider = provider;
	}




	@Override
	public void doubleClick(DoubleClickEvent event) {
		if(index == columns.length-1) return; // double click on last column. Do nothing.
		Map[] maps = provider.getValues();
		Map map = maps[index];
		ISelection selection = event.getSelection();
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection s = (IStructuredSelection) selection;
			Object o = s.getFirstElement();
			Set<Node> nodes = (Set<Node>) map.get(o);
			columns[index+1].setInput(nodes);
			columns[index+1].getTable().layout();
			
			for (int i = index+2; i < columns.length; i++) {
				columns[i].getTable().clearAll();
				columns[i].getTable().layout();
			}
			
		}
		

	}

}
