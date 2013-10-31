package damp.ekeko;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;


public class EkekoModel {
	
	private static Collection<IProjectModelFactory> factories = new ArrayList<IProjectModelFactory>();
	

	public static boolean registerFactory(IProjectModelFactory factory){
		return factories.add(factory);
	}
	
	public static boolean unregisterFactory(IProjectModelFactory factory){
		return factories.remove(factory);
	}
	
	public static void registerDefaultFactories(){
		IProjectModelFactory javaFactory = new JavaProjectModelFactory();
		EkekoModel.registerFactory(javaFactory);
	}
	
	
	public static void toggleNature(IProject project) {
		try {
			damp.util.Natures.toggleNature(project, EkekoNature.NATURE_ID);
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}
	
	private EkekoModel() {
		listeners = new HashSet<IEkekoModelUpdateListener>();
		clean();
	}
	
	// see http://en.wikipedia.org/wiki/Initialization_on_demand_holder_idiom
    private static class LazyHolder {
        public static final EkekoModel INSTANCE = new EkekoModel();
    }

    public static EkekoModel getInstance() {
		return LazyHolder.INSTANCE;
	}
			
    private Multimap<IProject, IProjectModel> projectModels = HashMultimap.create();
    
	private Set<IEkekoModelUpdateListener> listeners;
	
	public Collection<IProjectModel> getProjectModel(IProject ip) {
		return projectModels.get(ip);
	}
	
	public boolean hasProjectModel(IProject ip) {
		return !getProjectModel(ip).isEmpty();
	}
	
	public void removeProjectModels(IProject ip)  {
		System.out.println("Removing an existing project from the model: " + ip.toString());
		Collection<IProjectModel> removed = projectModels.get(ip);
		projectModels.removeAll(ip);
		for(IProjectModel m : removed)
			notifyListeners(new EkekoModelRemovedEvent(m));
	}
	
	public void removeProjectModel(IProject ip, IProjectModel m)  {
		Collection<IProjectModel> models = projectModels.get(ip);
		models.remove(m);	
		notifyListeners(new EkekoModelRemovedEvent(m));
	}
	
	public Collection<IProjectModel> getProjectModel(IResource rsc) {
		return getProjectModel(rsc.getProject());
	}

	public JavaProjectModel getJavaProjectModel(IJavaProject p) {
		return getJavaProjectModel(p.getResource());
	}
	
	public JavaProjectModel getJavaProjectModel(IResource rsc) {
		if(rsc == null)
			return null;
		//return (JavaProjectModel) getProjectModel(rsc);
		for (IProjectModel pm : getProjectModel(rsc)) {
			if (pm instanceof JavaProjectModel) {
				JavaProjectModel ijpm = (JavaProjectModel) pm;
				return ijpm;
			}
		}
		return null;
	}
	
	public Collection<IProjectModel> getProjectModels() {
		return projectModels.values();
	}

	public ITypeHierarchy getTypeHierarchy(IType type) throws JavaModelException {
		IJavaProject jp = type.getJavaProject();
		if(jp == null)
			return null;
		JavaProjectModel jpm = getJavaProjectModel(jp);
		if(jpm == null)
			return null;
		return jpm.getTypeHierarchy(type);
	}
	
	public void clean() {
		System.out.println("Cleaning the model.");
		projectModels.clear();
	}
	
	public void populate() {
		final String msg = "Populating Ekeko project models for queried projects.";
		System.out.println(msg);
		Job job = new Job(msg) {
			protected IStatus run(final IProgressMonitor m) {
				IProject[] iprojects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
				m.beginTask(msg, iprojects.length);
				for(IProject p : iprojects) {
					try {
						if(p.isOpen() && p.hasNature(EkekoNature.NATURE_ID)) 
							fullProjectBuild(p,null);
						m.worked(1);
					} catch (CoreException e) {
						e.printStackTrace();
					}
				}
				m.done();
				return Status.OK_STATUS;
			}
		};
		job.setUser(false);
		job.schedule();
	}
	
	private List<? extends IProjectModel> newProjectModel(IProject p) throws CoreException {		
		String[] natures = p.getDescription().getNatureIds();
		Set<IProjectModelFactory> applicableFactories = new HashSet<IProjectModelFactory>();
		for(int i = 0; i < natures.length; ++i){
			for(IProjectModelFactory fac : factories){				
				if(fac.applicableNatures().contains(natures[i])) {
					applicableFactories.add(fac);
				}
			}
		}

		
		//each factory is given the applicable factories for the project
		//are allowed to filter out factories they conflict with
		//intersection of all approved factories is the final one
		Set<IProjectModelFactory> approvedFactories = new HashSet<IProjectModelFactory>();
		approvedFactories.addAll(applicableFactories);
		for(IProjectModelFactory fac : applicableFactories) 
			approvedFactories.removeAll(fac.conflictingFactories(p, applicableFactories));
	
		
		if(approvedFactories.isEmpty()){
			return Arrays.asList(new ProjectModel(p));
		} else {
			ArrayList<IProjectModel> applicableProjectModels = new ArrayList<IProjectModel>(approvedFactories.size());
			for (IProjectModelFactory iProjectModelFactory : approvedFactories) {
				applicableProjectModels.add(iProjectModelFactory.createModel(p));
			}
			return applicableProjectModels;
		}
	}
	
	
	
	
	
	public void fullProjectBuild(IProject project, IProgressMonitor monitor) throws CoreException {
		if(project.isOpen()) {
			System.out.println("Adding a new project to the model: " + project.toString());
			List<? extends IProjectModel> models = newProjectModel(project);
			for (IProjectModel model : models) {
				model.populate(monitor);
				//if(!projectModels.containsEntry(project, model))
				addProjectModel(project, model);
			}
		}
	}

	private void addProjectModel(IProject project, IProjectModel model){
		//remove existing project models of the same class as model
		Collection<IProjectModel> models = projectModels.get(project);
		Iterator<IProjectModel> i = models.iterator();
		while(i.hasNext()) {
			IProjectModel m = i.next();
			if(m.getClass().equals(model.getClass()))
				i.remove(); //safe
		}		
		projectModels.put(project,model); 	
		//notify the model it has been added 
		model.addedToEkekoModel(this, models);
		//notify listeners
		notifyListeners(new EkekoModelAddedEvent(model));
	}
	
	public void incrementalProjectBuild(IResourceDelta delta, IProgressMonitor monitor) throws CoreException {
		IResource resource = delta.getResource();
		int resourceType = resource.getType();
		IProject ip = resource.getProject();	
		if(resourceType == IResource.PROJECT) {
			if (!ip.hasNature(EkekoNature.NATURE_ID)) {
				System.out.println("Removing project from model as nature was removed: " + ip.toString());
				removeProjectModels(ip);
				return;
			}					
			switch (delta.getKind()) {
			case IResourceDelta.DESCRIPTION:
				//TODO: check whether this really works ...
				//nature can have been added or removed
				System.out.println("Re-creating project models as its natures were changed: " + ip.toString());
				fullProjectBuild(ip,monitor);
				return;
			case IResourceDelta.ADDED:	
				fullProjectBuild(ip, monitor);
				return;
			case IResourceDelta.REMOVED:
				removeProjectModels(ip);
				return;
			}
		}
		System.out.println("Updating a project in the model: " + ip.toString());
		//TODO: take into account that project natures can have been added 
		for (IProjectModel ipm : getProjectModel(ip)) {
			ipm.processDelta(delta, monitor);
			notifyListeners(new EkekoModelChangedEvent(ipm));
		}
	}
	
	public boolean addListener(IEkekoModelUpdateListener l) {
		return listeners.add(l);
	}

	public boolean removeListener(IEkekoModelUpdateListener l) {
		return listeners.remove(l);
	}
	
	public void notifyListeners(EkekoModelUpdateEvent e) {
		for(IEkekoModelUpdateListener listener : listeners)
			listener.projectModelUpdated(e);
	}	
}

	
	
