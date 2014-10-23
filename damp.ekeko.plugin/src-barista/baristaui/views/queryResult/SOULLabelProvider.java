package baristaui.views.queryResult;

import java.util.Collection;
import java.util.List;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.ITreeListAdapter;
import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import clojure.lang.IFn;

public class SOULLabelProvider extends LabelProvider {

	public static IFn FN_ISWRAPPER;
	public static IFn FN_GETWRAPPEDVALUE;
	
	public static boolean isEkekoWrapperForValue(Object object) {
		return (Boolean) FN_ISWRAPPER.invoke(object);
	}
	
	public static Object getWrappedValue(Object wrapper) {
		return FN_GETWRAPPEDVALUE.invoke(wrapper);
	}
	
	
	@Override
	public String getText(Object element) {
		return prettyPrint(element);
	}
	
	
	public String prettyPrint(Object object) {
		if(object == null) {
			return "null";
		}
		if (object instanceof CompilationUnit) {
			CompilationUnit cu = (CompilationUnit) object;
			IJavaElement el = cu.getJavaElement();
			if(el != null){
				return el.getElementName();
			} else {
				List types = cu.types();
				String typeStr = prettyPrint(types.get(0));
				return "CompilationUnit " + cu.getPackage().getName().getFullyQualifiedName() + "." + typeStr;
			}
		}
		if (object instanceof AbstractTypeDeclaration) {
			AbstractTypeDeclaration typeDec = (AbstractTypeDeclaration) object;
			return typeDec.getName().getFullyQualifiedName();
		}
		
		if (object instanceof MethodDeclaration) {
			MethodDeclaration meth = (MethodDeclaration) object;
			String methName = meth.getName().getIdentifier();
			
			ASTNode parent = meth.getParent();
			if(parent instanceof TypeDeclaration) {
				TypeDeclaration typeDeclaration = (TypeDeclaration) parent;
				String className = typeDeclaration.getName().getIdentifier();
				return className + "." + methName;
			}
			IMethodBinding mb = meth.resolveBinding();
			if(mb != null) {
				ITypeBinding declaringClass = mb.getDeclaringClass();
				return declaringClass.getName() + "." + methName;
			} 
			return methName;
		}
		
		if (object instanceof ASTNode) {
			ASTNode node = (ASTNode) object;
			String txt = node.toString().replaceAll("\\r\\n|\\r|\\n", " ");
			//String txt = node.toString().split("\n")[0];
			//if(txt.length()>300){
			//	txt = txt.substring(0, 300);
			//}
			//return txt + " ...";
		}
		
		if(isEkekoWrapperForValue(object)) {
			Object value = getWrappedValue(object);
			if(value == null) {
				return "null";
			}
			return value.toString();
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
