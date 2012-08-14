package baristaui.views.queryResult;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.ITreeListAdapter;
import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

public class SOULLabelProvider extends LabelProvider {

	
	@Override
	public String getText(Object element) {
		return prettyPrint(element);
	}
	
	
	String prettyPrint(Object object) {
		if(object == null) {
			return "nil";
		}
		if (object instanceof CompilationUnit) {
			CompilationUnit cu = (CompilationUnit) object;
			return cu.getJavaElement().getElementName();
		}
		if (object instanceof AbstractTypeDeclaration) {
			AbstractTypeDeclaration typeDec = (AbstractTypeDeclaration) object;
			return typeDec.getName().getFullyQualifiedName();
		}
		
		if (object instanceof MethodDeclaration) {
			MethodDeclaration meth = (MethodDeclaration) object;
			return meth.getName().getFullyQualifiedName();
		}
		
		
		
		if (object instanceof ASTNode) {
			ASTNode node = (ASTNode) object;
			String txt = node.toString().split("\n")[0];
			if(txt.length()>150){
				txt = txt.substring(0, 150);
			}
			return txt+" ...";
		}
		
		return super.getText(object);
	}
	@Override
	public Image getImage(Object object) {
		if (object instanceof CompilationUnit) {
			return JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJS_CUNIT);
		}
		if (object instanceof AbstractTypeDeclaration) {
			return JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJS_CLASS);
		}
		
		if (object instanceof MethodDeclaration) {
			return JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJS_DEFAULT);
		}
		
		if (object instanceof FieldDeclaration) {
			return JavaUI.getSharedImages().getImage(ISharedImages.IMG_FIELD_DEFAULT);
		}
		return super.getImage(object);
	}
	

}
