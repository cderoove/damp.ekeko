package damp.ekeko;

import java.util.Iterator;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import damp.ekeko.soot.SootNature;
import damp.util.Natures;

public class ToggleEkekoNatureAction implements IObjectActionDelegate {

	private ISelection selection;

	public void run(IAction action) {
		if (selection instanceof IStructuredSelection) {
			for (Iterator<?> it = ((IStructuredSelection) selection).iterator(); it.hasNext();) {
				Object element = it.next();
				IProject project = null;
				if (element instanceof IProject) 
					project = (IProject) element;
				else if (element instanceof IAdaptable) 
					project = (IProject) ((IAdaptable) element).getAdapter(IProject.class);
				if (project != null) {
					try {
						//enable Ekeko nature if disabled
						if(project.hasNature(EkekoNature.NATURE_ID)) {
							//remove Soot nature first, because it requires Ekeko nature
							if(project.hasNature(SootNature.NATURE_ID))
								Natures.removeNature(project, SootNature.NATURE_ID);
							Natures.removeNature(project, EkekoNature.NATURE_ID);
						} else
							Natures.addNature(project, EkekoNature.NATURE_ID);
					} catch (CoreException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	public void selectionChanged(IAction action, ISelection selection) {
		this.selection = selection;
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}

	

}
