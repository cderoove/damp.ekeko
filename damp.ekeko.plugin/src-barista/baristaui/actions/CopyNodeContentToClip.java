package baristaui.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import baristaui.views.queryResult.tree.TreeResultContentProvider.Node;

public class CopyNodeContentToClip implements IObjectActionDelegate {

	private IStructuredSelection selection;


	@Override
	public void run(IAction action) {
		Object o = selection.getFirstElement();
		if (o instanceof Node) {
			Node node = (Node) o;
			Clipboard cb = new Clipboard(Display.getDefault());
			TextTransfer textTransfer = TextTransfer.getInstance();
			cb.setContents(new Object[] { node.from.toString() }, new Transfer[] { textTransfer });
		}
		
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
