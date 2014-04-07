package baristaui.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import clojure.lang.IFn;

public class InspectAction implements IObjectActionDelegate {
	
	public static IFn FN_INSPECT_USING_JAY;
	
	public static void openInspectorJay(Object o) {
		FN_INSPECT_USING_JAY.invoke(o);
	}

	private IStructuredSelection selection;

	@Override
	public void run(IAction action) {
		openInspectorJay(selection.getFirstElement());
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		this.selection = (IStructuredSelection) selection;
	}

	@Override
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		// TODO Auto-generated method stub
		
	}

}
