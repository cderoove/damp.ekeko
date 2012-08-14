package damp.ekeko;

import java.util.Collection;

import org.eclipse.core.resources.IProject;

public interface IProjectModelFactory {

	public IProjectModel createModel(IProject project);
	public Collection<String> applicableNatures();
}
