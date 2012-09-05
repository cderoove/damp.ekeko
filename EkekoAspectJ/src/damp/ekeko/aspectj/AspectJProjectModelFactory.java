package damp.ekeko.aspectj;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.ajdt.core.AspectJPlugin;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import damp.ekeko.IProjectModel;
import damp.ekeko.IProjectModelFactory;

public class AspectJProjectModelFactory implements IProjectModelFactory{

	@Override
	public IProjectModel createModel(IProject project) {
		return new AspectJProjectModel(project);
	}
	
	public Collection<String> applicableNatures() {
		Collection<String> result = new ArrayList<String>(1);
		result.add(AspectJPlugin.ID_NATURE);
		return result;
	}

}
