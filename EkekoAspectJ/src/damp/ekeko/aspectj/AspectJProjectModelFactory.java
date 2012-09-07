package damp.ekeko.aspectj;

import java.util.ArrayList;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.ajdt.core.AspectJPlugin;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import damp.ekeko.IProjectModel;
import damp.ekeko.IProjectModelFactory;
import damp.ekeko.JavaProjectModelFactory;

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

	@Override
	public Collection<IProjectModelFactory> conflictingFactories(IProject p, Collection<IProjectModelFactory> applicableFactories) {
		Set<IProjectModelFactory> conflicts = new HashSet<IProjectModelFactory>();
		for(IProjectModelFactory f : applicableFactories)
			if (f instanceof JavaProjectModelFactory)
				conflicts.add(f);
		return conflicts;
		
		//nifty, but introduces dependency on Guava
		//revert if starting to use Guava elsewhere
		//return Collections2.filter(applicableFactories, Predicates.instanceOf(JavaProjectModelFactory.class));
	}

}
