package damp.ekeko.soot;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

import damp.ekeko.Activator;
import damp.ekeko.EkekoNature;

public class SootNature implements IProjectNature{

	public static final String NATURE_ID = "damp.ekeko.plugin.sootNature";

	private IProject project;

	@Override
	public void configure() throws CoreException {
		//only add if project has the ekeko nature
		if(!damp.util.Natures.hasNature(project, EkekoNature.NATURE_ID)) {
			damp.util.Natures.removeNature(project, NATURE_ID);	
			return;
		}
		//remove all other soot natures (as soot itself relies on global state)
		//TODO: find workaround
		for(IProject p : ResourcesPlugin.getWorkspace().getRoot().getProjects())
			if(p != project) {
				try {
					damp.util.Natures.removeNature(p, NATURE_ID);	
				} catch (CoreException e) {
					e.printStackTrace();
				}}
		Activator.getConsoleStream().println("SOOT nature added to: " + project.getName());
	}

	@Override
	public void deconfigure() throws CoreException {
		Activator.getConsoleStream().println("SOOT nature nature removed from: " + project.getName());

	}

	@Override
	public IProject getProject() {
		return project;
	}

	@Override
	public void setProject(IProject project) {
		this.project = project;
	}


}



