package damp.ekeko.soot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.eclipse.core.resources.IProject;

import damp.ekeko.IProjectModel;
import damp.ekeko.IProjectModelFactory;

public class SootProjectModelFactory implements IProjectModelFactory{

	@Override
	public IProjectModel createModel(IProject project) {
		return new SootProjectModel(project);
	}
	
	public Collection<String> applicableNatures() {
		Collection<String> result = new ArrayList<String>(1);
		result.add(SootNature.NATURE_ID);
		return result;
	}

	@Override
	public Collection<IProjectModelFactory> conflictingFactories(IProject p, Collection<IProjectModelFactory> applicableFactories) {
		return Collections.emptySet();
	}

}
