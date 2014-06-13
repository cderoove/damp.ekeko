package damp.ekeko;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.eclipse.core.resources.IProject;

public class ProjectModelFactory implements IProjectModelFactory{

	public IProjectModel createModel(IProject project) {
		return new ProjectModel(project);
	}

	public Collection<String> applicableNatures(){
		return new ArrayList<String>(0);
	}

	public Collection<IProjectModelFactory> conflictingFactories(IProject p, Collection<IProjectModelFactory> applicableFactories) {
		return Collections.emptySet();
	}
	
}
