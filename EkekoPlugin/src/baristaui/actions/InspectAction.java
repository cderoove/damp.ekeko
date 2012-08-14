package baristaui.actions;

import org.eclipse.jdt.astview.ASTViewPlugin;
import org.eclipse.jdt.astview.views.ASTView;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import baristaui.util.MarkerUtility;
import baristaui.views.queryResult.tree.TreeResultContentProvider.Node;

public class InspectAction implements IObjectActionDelegate {

	private IStructuredSelection selection;

	@Override
	public void run(IAction action) {
		Object o = selection.getFirstElement();
		if (o instanceof Node) {
			Node theNode = (Node) o;
			if(theNode.from instanceof ASTNode) {
				ASTNode ast = (ASTNode) theNode.from;
				MarkerUtility.getInstance().createMarkerAndGoto(ast);
				IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				try {
					activePage.showView("org.eclipse.jdt.astview.views.ASTView");
					
				} catch (PartInitException e) {
					e.printStackTrace();
				}
												
			} 
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
