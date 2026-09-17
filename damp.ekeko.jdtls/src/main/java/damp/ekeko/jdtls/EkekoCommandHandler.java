package damp.ekeko.jdtls;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ls.core.internal.IDelegateCommandHandler;

import damp.ekeko.EkekoModel;
import damp.ekeko.EkekoNature;
import damp.util.Natures;

/**
 * Dispatches Ekeko's {@code workspace/executeCommand} commands, registered
 * via the {@code org.eclipse.jdt.ls.core.delegateCommandHandler} extension
 * point in plugin.xml. Reachable from a client as
 * {@code java.execute.workspaceCommand} with one of the command ids below.
 */
public class EkekoCommandHandler implements IDelegateCommandHandler {

	private static final String REBUILD_MODEL = "ekeko.rebuildModel";

	@Override
	public Object executeCommand(String commandId, List<Object> arguments, IProgressMonitor monitor) throws Exception {
		switch (commandId) {
		case REBUILD_MODEL:
			return rebuildModel(monitor);
		default:
			throw new UnsupportedOperationException("Unsupported command: " + commandId);
		}
	}

	private Object rebuildModel(IProgressMonitor monitor) throws Exception {
		List<String> rebuiltProjects = new ArrayList<>();
		for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
			if (!project.isOpen() || !project.hasNature(JavaCore.NATURE_ID)) {
				continue;
			}
			if (!project.hasNature(EkekoNature.NATURE_ID)) {
				Natures.addNature(project, EkekoNature.NATURE_ID);
			}
			EkekoModel.getInstance().fullProjectBuild(project, monitor);
			rebuiltProjects.add(project.getName());
		}
		return rebuiltProjects;
	}
}
