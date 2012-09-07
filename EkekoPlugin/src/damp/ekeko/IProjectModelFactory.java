package damp.ekeko;

import java.util.Collection;
import java.util.Set;

import org.eclipse.core.resources.IProject;

public interface IProjectModelFactory {

	public IProjectModel createModel(IProject project);
	public Collection<String> applicableNatures();
	//enables a factory to veto other factories for a particular project
	public Collection<IProjectModelFactory> conflictingFactories(IProject p, Collection<IProjectModelFactory> applicableFactories);
}
