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

import damp.ekeko.soot.SootNature;


public class DisableNatureOnAllHandler extends AbstractHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {	
		try {
			//remove Soot nature first because it requires the Ekeko nature
			damp.util.Natures.removeNatureFromAllProjects(SootNature.NATURE_ID);
			damp.util.Natures.removeNatureFromAllProjects(EkekoNature.NATURE_ID);
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return null;	
	}

}

