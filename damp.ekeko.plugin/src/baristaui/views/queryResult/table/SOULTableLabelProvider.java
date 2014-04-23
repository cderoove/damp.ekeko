package baristaui.views.queryResult.table;

import java.util.Map;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import baristaui.views.queryResult.SOULLabelProvider;

public class SOULTableLabelProvider extends ColumnLabelProvider {

	private String key;
	
	private LabelProvider soulProvider;
	
	public SOULTableLabelProvider(String key, LabelProvider p) {
		super();
		soulProvider = p;
		this.key = key;

	}

			
	@Override
	public Image getImage(Object element) {
		Object object = null;
		
		if (element instanceof Map) {
			Map m = (Map) element;
			object = m.get(key);
		}else{
			return null;
		}
		
		return soulProvider.getImage(object);
		
	}

	@Override
	public String getText(Object element) {
		if (element instanceof Map) {
			Map map = (Map) element;
			return soulProvider.getText(map.get(key));
		}
				
		return super.getText(element);
	}
   
}
