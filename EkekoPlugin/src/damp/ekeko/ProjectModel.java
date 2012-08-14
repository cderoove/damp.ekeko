package damp.ekeko;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

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

}