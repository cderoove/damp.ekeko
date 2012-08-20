package damp.ekeko;

import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

public interface IProjectModel {
	
	abstract public void clean();
	
	abstract public void populate(IProgressMonitor monitor) throws CoreException;

	abstract public void processDelta(IResourceDelta delta, IProgressMonitor monitor) throws CoreException;

}
