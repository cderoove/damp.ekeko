package damp.ekeko.jdtls;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.JavaCore;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import damp.ekeko.EkekoModel;
import damp.ekeko.EkekoNature;
import damp.util.Natures;

/**
 * Activates Ekeko's query engine (reused from damp.ekeko.plugin) inside
 * the jdt.ls OSGi runtime.
 *
 * damp.ekeko.plugin normally lets a user opt individual projects into
 * Ekeko via a menu-toggled EkekoNature (damp.ekeko.EkekoNature), which,
 * once enabled, registers EkekoBuilder into that project's build spec so
 * Ekeko's project models are populated/updated via Eclipse's normal
 * incremental build machinery (EkekoModel.fullProjectBuild/
 * incrementalProjectBuild). jdt.ls has no such menu, so this activator
 * enables that same nature automatically on every open Java project
 * instead, both at startup and as projects are opened/created going
 * forward, leaving EkekoModel/EkekoBuilder/EkekoNature themselves
 * unchanged. No custom IResourceChangeListener-based rebuild trigger is
 * needed beyond that: enabling the nature is enough to hook Ekeko into
 * the build lifecycle jdt.ls already runs for its own diagnostics.
 */
public class EkekoJdtlsActivator implements BundleActivator {

	private IResourceChangeListener projectOpenListener;

	@Override
	public void start(BundleContext context) throws Exception {
		EkekoModel.registerDefaultFactories();

		// Register the listener before the initial scan so a project
		// import racing with bundle activation is still caught one way or
		// the other.
		projectOpenListener = new EkekoNatureEnablingListener();
		ResourcesPlugin.getWorkspace().addResourceChangeListener(projectOpenListener, IResourceChangeEvent.POST_CHANGE);

		// This bundle activates very early in jdt.ls's own startup
		// sequence -- before it has necessarily finished importing
		// whatever project(s) the client opened -- so an immediate scan of
		// ResourcesPlugin.getWorkspace().getRoot().getProjects() can find
		// nothing yet. Retry the initial scan a few times with a short
		// delay instead of assuming it succeeds on the first attempt;
		// projects opened/added after this point are still covered by the
		// listener above regardless.
		new EnableNatureOnKnownProjectsJob().schedule(500);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		if (projectOpenListener != null) {
			ResourcesPlugin.getWorkspace().removeResourceChangeListener(projectOpenListener);
			projectOpenListener = null;
		}
	}

	private static void enableEkekoNatureIfJavaProject(IProject project) {
		try {
			if (project.isOpen() && project.hasNature(JavaCore.NATURE_ID) && !project.hasNature(EkekoNature.NATURE_ID)) {
				Natures.addNature(project, EkekoNature.NATURE_ID);
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	private static final class EnableNatureOnKnownProjectsJob extends Job {

		private static final int MAX_ATTEMPTS = 10;
		private int attempt = 0;

		EnableNatureOnKnownProjectsJob() {
			super("Enabling Ekeko on open Java projects");
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			attempt++;
			IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
			for (IProject project : projects) {
				enableEkekoNatureIfJavaProject(project);
			}
			// EkekoModel.populate() itself only builds projects that already
			// have the nature, so it's safe (and cheap when there's nothing
			// to do) to call on every attempt.
			EkekoModel.getInstance().populate();
			if (projects.length == 0 && attempt < MAX_ATTEMPTS) {
				schedule(1000);
			}
			return Status.OK_STATUS;
		}
	}

	private static class EkekoNatureEnablingListener implements IResourceChangeListener {
		@Override
		public void resourceChanged(IResourceChangeEvent event) {
			IResourceDelta delta = event.getDelta();
			if (delta == null) {
				return;
			}
			try {
				delta.accept(new IResourceDeltaVisitor() {
					@Override
					public boolean visit(IResourceDelta d) {
						if (d.getResource() instanceof IProject) {
							enableEkekoNatureIfJavaProject((IProject) d.getResource());
						}
						return true;
					}
				});
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
	}
}
