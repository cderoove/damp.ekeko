package damp.ekeko.soot;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.SelectionDialog;

import damp.ekeko.EkekoProjectPropertyPage;
import damp.ekeko.soot.SootNature;
import damp.util.Natures;

public class ToggleSootNatureAction implements IObjectActionDelegate {

	private ISelection selection;

	public static IType chooseEntryPoint(Shell shell, IJavaProject javaProject) {
		IJavaSearchScope searchScope = null;
		if ((javaProject == null) || !javaProject.exists()) {
			searchScope = SearchEngine.createWorkspaceScope();
		} else {
			searchScope = SearchEngine.createJavaSearchScope(new IJavaElement[] {javaProject}, false);
		}		

		int constraints = IJavaElementSearchConstants.CONSIDER_BINARIES;

		SelectionDialog dialog = JavaUI.createMainTypeDialog(shell, PlatformUI.getWorkbench().getActiveWorkbenchWindow(), searchScope, constraints,  false); 

		dialog.setTitle("Choose Main Type"); 
		dialog.setMessage("Choose a main type as the entry point for static analyses:"); 
		if (dialog.open() == Window.CANCEL) {
			return null;
		}

		IProject project = javaProject.getProject();

		try {
			String entry = getPersistentEntryPoint(project);
			if(entry != null) {
				IProgressMonitor m = null; //because findType is overloaded on second parameter
				IType entryType = javaProject.findType(entry, m);
				if(entryType != null) {
					Object[] selectedElements = {entryType};
					dialog.setInitialSelections(selectedElements);
				}

			}
		} catch (JavaModelException e) {
			e.printStackTrace();
		} catch (CoreException e) {
			e.printStackTrace();
		}

		Object[] results = dialog.getResult();
		if ((results == null) || (results.length < 1)) {
			return null;
		}		

		IType selected = (IType)results[0];
		
		try {
			setPersistentEntryPoint(project,selected.getFullyQualifiedName());
		} catch (CoreException e) {
			e.printStackTrace();
		}
		
		return selected;
	}

	public static void setPersistentEntryPoint(IProject project, String entryPoint) throws CoreException  {
		project.setPersistentProperty(EkekoProjectPropertyPage.ENTRYPOINT_PROPERTY, entryPoint);
	}

	public static String getPersistentEntryPoint(IProject project) throws CoreException {
		return project.getPersistentProperty(EkekoProjectPropertyPage.ENTRYPOINT_PROPERTY);
	}

	public void run(IAction action) {
		if (selection instanceof IStructuredSelection) {
			for (Iterator<?> it = ((IStructuredSelection) selection).iterator(); it.hasNext();) {
				Object element = it.next();
				IProject project = null;
				if (element instanceof IProject) 
					project = (IProject) element;
				else if (element instanceof IAdaptable) 
					project = (IProject) ((IAdaptable) element).getAdapter(IProject.class);
				if (project != null) {
					try {
						if(project.hasNature(SootNature.NATURE_ID)) 
							Natures.removeNature(project, SootNature.NATURE_ID);
						else {
							IJavaProject jp = JavaCore.create(project);
							IType selectedType = chooseEntryPoint(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),jp);
							if(selectedType != null) { 
								Natures.addNature(project, SootNature.NATURE_ID);
								//build from scratch because Ekeko does not yet handle natures added to an existing project correctly
								project.build(IncrementalProjectBuilder.FULL_BUILD, null);
							}
						}	
					} catch (CoreException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	public void selectionChanged(IAction action, ISelection selection) {
		this.selection = selection;
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}



}
