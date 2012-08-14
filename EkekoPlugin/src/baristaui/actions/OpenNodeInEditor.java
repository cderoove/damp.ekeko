package baristaui.actions;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;

import baristaui.util.MarkerUtility;
import baristaui.views.queryResult.tree.TreeResultContentProvider.Node;

public class OpenNodeInEditor implements IObjectActionDelegate{

	private IStructuredSelection selection;

	public OpenNodeInEditor() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void run(IAction action) {
		Object o = selection.getFirstElement();
		
		ASTNode ast = null;
		
		if (o instanceof Node) {
			Node theNode = (Node) o;
			o = theNode.from;
		}
		
		
		if (o instanceof ASTNode) {
			ASTNode theNode = (ASTNode) o;
			try {
				showNodeInEditor(theNode);
			} catch (PartInitException e) {
				e.printStackTrace();
			} catch (JavaModelException e) {
				e.printStackTrace();
			}
		}
	}
	
	protected void showNodeInEditor(final ASTNode resultASTNode) throws PartInitException, JavaModelException {
			
		MarkerUtility.getInstance().createMarkerAndGoto(resultASTNode);
//		ASTNode root = resultASTNode.getRoot();
//		if (root instanceof CompilationUnit) {
//			CompilationUnit cu = (CompilationUnit) root;
//			IJavaElement rootJE = cu.getJavaElement();
//			if (rootJE instanceof ICompilationUnit) {
//				ICompilationUnit cuJE = (ICompilationUnit) rootJE;
//				IJavaElement element = cuJE.getElementAt(resultASTNode.getStartPosition());
//				IEditorPart editor = JavaUI.openInEditor(element);
//			}
//			
//						
//		}
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
