package damp.ekeko;

import java.util.Collection;

import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
/**
 * Interface for models gather by EkekoModel for each IProject that has an EkekoNature. 
 * 
 * @author cderoove
 *
 */
public interface IProjectModel {
	
	/**
	 * Called by EkekoModel when the receiver has to be cleaned. 
	 */
	abstract public void clean();
	
	/**
	 * Called by EkekoModel when the receiver has to be populated with information about its IProject.
	 * 
	 * @param monitor
	 * @throws CoreException
	 */
	abstract public void populate(IProgressMonitor monitor) throws CoreException;

	/**
	 * Called by EkekoModel when the receiver has to update its IProject information 
	 * according to the given IResourceDelta.
	 * 
	 * @param delta
	 * @param monitor
	 * @throws CoreException
	 */
	abstract public void processDelta(IResourceDelta delta, IProgressMonitor monitor) throws CoreException;

	/**
	 * Called by EkekoModel when the receiver has been added to the collection of 
	 * IProjectModel instances for an IProject. 
	 * 
	 * @param m  
	 * the EkekoModel the receiver has been added to
	 * @param 
	 * otherModelsForProject the collection of IProjectModel instances for the IProject, including the receiver.
	 */
	abstract public void addedToEkekoModel(EkekoModel m, Collection<IProjectModel> otherModelsForProject);

}
