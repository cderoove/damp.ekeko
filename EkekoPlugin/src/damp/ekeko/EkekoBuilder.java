package damp.ekeko;

import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

public class EkekoBuilder extends IncrementalProjectBuilder {
	
	/*
	 * NOTE: The builder name in a build command is the fully qualified id of the builder extension. The fully qualified id of an extension is created by combining the plug-in id with the simple extension id in the plugin.xml file. For example, a builder with simple extension id "mybuilder" in the plug-in "com.example.builders" would have the name "com.example.builders.mybuilder"
	 */
	public static final String BUILDER_ID = "damp.ekeko.plugin.ekekoBuilder";
	
	
		
	protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
		if (kind == FULL_BUILD || !(EkekoModel.getInstance().hasProjectModel(getProject()))) {
			fullBuild(monitor);
		} else {
			IResourceDelta delta = getDelta(getProject());
			if (delta == null) {
				fullBuild(monitor);
			} else {
				incrementalBuild(delta, monitor);
			}
		}
		return null;
	}

	protected void fullBuild(final IProgressMonitor monitor) throws CoreException {
		EkekoModel.getInstance().fullProjectBuild(getProject(), monitor);
	}

	protected void incrementalBuild(IResourceDelta delta, IProgressMonitor monitor) throws CoreException {
		EkekoModel.getInstance().incrementalProjectBuild(delta, monitor);			
	}

	protected void startupOnInitialize() {
		// add builder init logic here
		// overriders must call super
		super.startupOnInitialize();
	}

	protected void clean(IProgressMonitor monitor) {
		// add builder clean logic here
		// super does nothing
		EkekoModel.getInstance().clean();
	}

}
