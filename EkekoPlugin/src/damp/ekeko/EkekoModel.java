package damp.ekeko;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;


public class EkekoModel {
	
	private static Collection<IProjectModelFactory> factories =
			new ArrayList<IProjectModelFactory>();
	
	
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
		clean();
	}
	
	// see http://en.wikipedia.org/wiki/Initialization_on_demand_holder_idiom
    private static class LazyHolder {
        public static final EkekoModel INSTANCE = new EkekoModel();
    }

    public static EkekoModel getInstance() {
		return LazyHolder.INSTANCE;
	}
		
	//private java.util.concurrent.ConcurrentHashMap<IProject,IProjectModel> projectModels;	
	
    private Multimap<IProject, IProjectModel> projectModels = HashMultimap.create();
    
	private IProject wholeProgramAnalysisProject;
	
	public IProject getWholeProgramAnalysisProject() {
		return wholeProgramAnalysisProject;
	}
	
	public boolean hasWholeProgramAnalysisProject() {
		return wholeProgramAnalysisProject != null;
	}
	
	public IProjectModel getWholeProgramAnalysisProjectModel() {
		if(!hasWholeProgramAnalysisProject())
			return null;
		for (IProjectModel ipm : getProjectModel(getWholeProgramAnalysisProject())) {
			return ipm; //first one we find
		}
		return null;
	}

	public void setWholeProgramAnalysisProject(IProject project) throws CoreException {
		if(project == null) {
			if(hasWholeProgramAnalysisProject()) {
				IProject old = getWholeProgramAnalysisProject();
				rebuildModel(old,null); //reset
				return;
			} 
		}
		rebuildModel(project,project);
	}
	
	private void rebuildModel(IProject project, IProject newWholeProgramAnalysisProject) throws CoreException {
		if(project.hasNature(EkekoNature.NATURE_ID)) 
			toggleNature(project); //removes the project's model
		wholeProgramAnalysisProject = newWholeProgramAnalysisProject;
		toggleNature(project); //(re)-builds the project's model
	}

	
	public Collection<IProjectModel> getProjectModel(IProject ip) {
		return projectModels.get(ip);
	}
	
	public boolean hasProjectModel(IProject ip) {
		return getProjectModel(ip) != null;
	}
	
	public void removeProjectModel(IProject ip)  {
		System.out.println("Removing an existing project from the model: " + ip.toString());
		projectModels.removeAll(ip);
		if(hasWholeProgramAnalysisProject() 
				&& getWholeProgramAnalysisProject().equals(ip))
			wholeProgramAnalysisProject = null;
	
	}
	
	public Collection<IProjectModel> getProjectModel(IResource rsc) {
		return getProjectModel(rsc.getProject());
	}
	
	public JavaProjectModel getJavaProjectModel(IJavaProject p) {
		IResource rsc = p.getResource();
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
		System.out.println("Populating model from enabled natures.");
		IProject[] iprojects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for(IProject p : iprojects) {
			try {
				if(p.isOpen() && p.hasNature(EkekoNature.NATURE_ID)) 
					fullProjectBuild(p,null);
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
	}
	
	private List<? extends IProjectModel> newProjectModel(IProject p) throws CoreException {
		if(p.equals(getWholeProgramAnalysisProject()))
			return Arrays.asList(new WholeProgramAnalysisJavaProjectModel(p));
		
		String[] natures = p.getDescription().getNatureIds();
		ArrayList<IProjectModelFactory> applicableFactories = new ArrayList<IProjectModelFactory>();
		for(int i = 0; i < natures.length; ++i){
			for(IProjectModelFactory fac : factories){
				if(fac.applicableNatures().contains(natures[i])){
					applicableFactories.add(fac);
				}
			}
		}
		if(applicableFactories.isEmpty()){
			return Arrays.asList(new ProjectModel(p));
		} else {
			ArrayList<IProjectModel> applicableProjectModels = new ArrayList<IProjectModel>(applicableFactories.size());
			for (IProjectModelFactory iProjectModelFactory : applicableFactories) {
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
		Collection<IProjectModel> models = projectModels.get(project);
		for(IProjectModel m: models){
			if(m.getClass().equals(model.getClass())){
				projectModels.remove(project, m);
			}
		}	
		projectModels.put(project,model); 
		
	}
	
	public void incrementalProjectBuild(IResourceDelta delta, IProgressMonitor monitor) throws CoreException {
		IResource resource = delta.getResource();
		int resourceType = resource.getType();
		IProject ip = resource.getProject();	
		if(resourceType == IResource.PROJECT) {
			if (!ip.hasNature(EkekoNature.NATURE_ID)) {
				System.out.println("Removing project from model as nature was removed: " + ip.toString());
				removeProjectModel(ip);
				return;
			}					
			switch (delta.getKind()) {
			case IResourceDelta.ADDED:	
				fullProjectBuild(ip, monitor);
				return;
			case IResourceDelta.REMOVED:
				removeProjectModel(ip);
				return;
			}
		}
		System.out.println("Updating a project in the model: " + ip.toString());
		for (IProjectModel ipm : getProjectModel(ip)) {
			ipm.processDelta(delta, monitor);
		}
	}
}

	
	
