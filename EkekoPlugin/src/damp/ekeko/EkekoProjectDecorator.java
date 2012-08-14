package damp.ekeko;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;

public class EkekoProjectDecorator extends LabelProvider implements  ILightweightLabelDecorator {

	private ImageDescriptor icon = Activator.getImageDescriptor("icons/ekeko7.png");
	
	
	public void decorate(Object element, IDecoration decoration) {
		if (element instanceof IProject) {
			
			IProject project = (IProject) element;
			try {
				IProjectNature pn = project.getNature(EkekoNature.NATURE_ID);
				if (pn != null) {
					
					decoration.addOverlay(icon, IDecoration.TOP_LEFT);
					
					decoration.addSuffix(" Ekeko");
					
				}
				
			} catch (CoreException e) {
				return;
			}
		}
	}


}
