package damp.ekeko;

import java.util.Collection;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import damp.util.Natures;

public class ProjectModel implements IProjectModel {

	private IProject project;
		
	public ProjectModel(IProject p) {
		project = p;
	}
	
	public IProject getProject() {
		return project;
	}
	
	public void setProject(IProject project) {
		this.project = project;
	}
	
	public void clean() {
	}
	
	public void populate(IProgressMonitor monitor) throws CoreException {
	}

	public void processDelta(IResourceDelta delta, IProgressMonitor monitor) throws CoreException {
	}
	
	public void addedToEkekoModel(EkekoModel m, Collection<IProjectModel> projectModels) {
	}
	
	protected void buildCanceled()  {
		clean();
		try {
			Natures.removeNature(project, EkekoNature.NATURE_ID);
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}


}