package damp.ekeko;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;


public class EnableNatureOnAllHandler extends AbstractHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {	

		IProject[] iprojects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for(IProject p : iprojects) {
			if(p.isOpen()) {
				try {
					IProjectDescription description = p.getDescription();
					String[] natures = description.getNatureIds();
					if(!p.hasNature(EkekoNature.NATURE_ID)) {
						String[] newNatures = new String[natures.length + 1];
						System.arraycopy(natures, 0, newNatures, 0, natures.length);
						newNatures[natures.length] = EkekoNature.NATURE_ID;
						description.setNatureIds(newNatures);
						p.setDescription(description, null);
					}
				} catch (CoreException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return null;	
	}
}

