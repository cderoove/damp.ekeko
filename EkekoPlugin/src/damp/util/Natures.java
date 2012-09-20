package damp.util;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import damp.ekeko.EkekoNature;


public class Natures {

	public static void addNatureToAllProjects(String natureId) throws CoreException {
		IProject[] iprojects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		addNatureToProjects(iprojects, natureId);
	}
	
	public static void addNatureToProjects(IProject[] iprojects, String natureId) throws CoreException {
		for(IProject p : iprojects)
			addNature(p, natureId);
	}

	
	public static void addNature(IProject project, String natureId) throws CoreException {
		addNature(project, natureId, null);
	}
	
	public static void addNature(IProject project, String natureId, IProgressMonitor monitor) throws CoreException{
		if(!project.exists())
			return;
		if(!project.isOpen())
			return;
		if(project.hasNature(natureId))
			return;
		
		IProjectDescription description = project.getDescription();
		String[] natureIds = description.getNatureIds();
		

		
		String[] newNatures = new String[natureIds.length + 1];
		System.arraycopy(natureIds, 0, newNatures, 0, natureIds.length);
		newNatures[natureIds.length] = natureId;
		description.setNatureIds(newNatures);
		project.setDescription(description, monitor);
	}
	
	
	
	public static void removeNatureFromAllProjects(String natureId) throws CoreException {
		IProject[] iprojects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		removeNatureFromProjects(iprojects, natureId);
	}
	
	public static void removeNatureFromProjects(IProject[] iprojects, String natureId) throws CoreException {
		for(IProject p : iprojects)
			removeNature(p, natureId);
	}

	public static void removeNature(IProject project, String natureId) throws CoreException{
		removeNature(project, natureId, null);
	}
	
	
	public static void removeNature(IProject project, String natureId, IProgressMonitor monitor) throws CoreException{
		if(!project.exists())
			return;
		if(!project.isOpen())
			return;
		if(!project.hasNature(natureId))
			return;
		
		IProjectDescription description = project.getDescription();
		String[] natureIds = description.getNatureIds();
		
		
		for (int i = 0; i < natureIds.length; ++i) {
			if (natureId.equals(natureIds[i])) {
				// Remove the nature
				String[] newNatures = new String[natureIds.length - 1];
				System.arraycopy(natureIds, 0, newNatures, 0, i);
				System.arraycopy(natureIds, i + 1, newNatures, i,
						natureIds.length - i - 1);
				description.setNatureIds(newNatures);
				project.setDescription(description, monitor);
				return;
			}
		}
	}
	
	public static boolean hasNature(IProject project, String natureId) throws CoreException {
		return project.hasNature(natureId);
	}
	
	public static void toggleNature(IProject project, String natureId) throws CoreException{
		toggleNature(project, natureId, null);
	}
	
	public static void toggleNature(IProject project, String natureId, IProgressMonitor monitor) throws CoreException{
		assert(project.exists());
		assert(project.isOpen());
		if(project.hasNature(natureId)){
			removeNature(project, natureId, monitor);
		} else {
			addNature(project, natureId, monitor);
		}
	}
	
	public static void ensureNature(IProject project, String natureId) throws CoreException {
		assert(project.exists());
		assert(project.isOpen());
		if(!project.hasNature(natureId))
			addNature(project, natureId);
	}
	
}
