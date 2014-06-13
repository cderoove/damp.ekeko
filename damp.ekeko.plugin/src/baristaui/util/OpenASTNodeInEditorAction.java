package baristaui.util;

import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;

public class OpenASTNodeInEditorAction extends Action {
	/**
	 * 
	 */
	private final StructuredViewer viewer;

	/**
	 * @param simpleQueryResultView
	 */
	public OpenASTNodeInEditorAction(StructuredViewer simpleQueryResultView) {
		this.viewer = simpleQueryResultView;
	}

	public void run() {
		ISelection selection = this.viewer.getSelection();
		IStructuredSelection structSelection = (IStructuredSelection)selection;
		Object obj = structSelection.getFirstElement();
		
		
		
		if (obj instanceof ASTNode) {
			ASTNode node = (ASTNode) obj;
			try {
				this.showNodeInEditor(node);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	protected void showNodeInEditor(final ASTNode resultASTNode) throws PartInitException, JavaModelException {
		ASTNode root = resultASTNode.getRoot();
		if (root instanceof CompilationUnit) {
			CompilationUnit cu = (CompilationUnit) root;
			IEditorPart editor = JavaUI.openInEditor(cu.getJavaElement());
			JavaUI.revealInEditor(editor, new ISourceReference() {
				
				@Override
				public ISourceRange getSourceRange() throws JavaModelException {
					// TODO Auto-generated method stub
					return new ISourceRange() {
						
						@Override
						public int getOffset() {
							// TODO Auto-generated method stub
							return resultASTNode.getStartPosition();
						}
						
						@Override
						public int getLength() {
							// TODO Auto-generated method stub
							return resultASTNode.getLength();
						}
					};
				}
				
				@Override
				public String getSource() throws JavaModelException {
					// TODO Auto-generated method stub
					return resultASTNode.toString();
				}
				
				@Override
				public boolean exists() {
					// TODO Auto-generated method stub
					return true;
				}

				@Override
				public ISourceRange getNameRange() throws JavaModelException {
					// TODO Auto-generated method stub
					return null;
				}
			});
		}
	}
}