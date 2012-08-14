package damp.ekeko;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.JavaCore;

public class JavaProjectModelFactory implements IProjectModelFactory {

	@Override
	public IProjectModel createModel(IProject project) {
		return new JavaProjectModel(project);
	}

	public Collection<String> applicableNatures(){
		Collection<String> result =  new ArrayList<String>(1);
		result.add(JavaCore.NATURE_ID);
		return result;
	}
}
