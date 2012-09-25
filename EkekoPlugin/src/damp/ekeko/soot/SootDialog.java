package damp.ekeko.soot;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.internal.core.JavaProject;
import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.dialogs.SelectionDialog;

import damp.ekeko.EkekoModel;
import damp.ekeko.EkekoNature;
import damp.ekeko.EkekoProjectPropertyPage;

public class SootDialog extends TitleAreaDialog {


	private Text projectNameText;
	private Text entryPointText;
	private String projectName;
	private String entryPoint;
	

	public SootDialog(Shell parentShell) {
		super(parentShell);
	}

	@Override
	public void create() {
		super.create();
		// Set the title
		setTitle("Ekeko Configuration for Whole-Program Analyses");
		// Set the message
		setMessage("To enable whole-program analyses for this Ekeko session, please select a Java project and its entry point. ",IMessageProvider.INFORMATION);
		
		//hole-program analyses need an entry point to the program under investigation. " +
//The latter is also stored in the project-specific Ekeko settings. 
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		// layout.horizontalAlignment = GridData.FILL;
		parent.setLayout(layout);

		// The text fields will grow with the size of the dialog
		GridData gridData = new GridData();
		gridData.grabExcessHorizontalSpace = true;
		gridData.horizontalAlignment = GridData.FILL;

		Label label1 = new Label(parent, SWT.NONE);
		label1.setText("Project Name");

		projectNameText = new Text(parent, SWT.BORDER);
		projectNameText.setLayoutData(gridData);
				
		
		updateProjectNameText(getModelProject());
		
		
		Button projectSelectButton = new Button(parent, SWT.PUSH);
		projectSelectButton.setText("Select...");
		
		projectSelectButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleProjectButtonSelected();
			}});
		
		GridData buttonGD = new GridData();
		projectSelectButton.setLayoutData(buttonGD);
		

		
		Label label2 = new Label(parent, SWT.NONE);
		label2.setText("Entry Point");
		// You should not re-use GridData
		gridData = new GridData();
		gridData.grabExcessHorizontalSpace = true;
		gridData.horizontalAlignment = GridData.FILL;
		entryPointText = new Text(parent, SWT.BORDER);
		entryPointText.setLayoutData(gridData);
		
		updateEntryPointText(getModelProject());

		
				
		Button selectEntryPointButton = new Button(parent, SWT.PUSH);
		selectEntryPointButton.setText("Select...");
		
		selectEntryPointButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleEntryPointButtonSelected();
			}
			});
		
		GridData buttonEPGD = new GridData();
		selectEntryPointButton.setLayoutData(buttonEPGD);
		
		return parent;
	}

	
	protected void handleProjectButtonSelected() {
		IProject project = chooseProject();
		if(project == null)
			return;
		updateEntryPointText(project);
		updateProjectNameText(project);
	}

	private void updateProjectNameText(IProject project) {
		String name = "";
		if(project != null)
			name = project.getName();
		projectNameText.setText(name);
	}

	private void updateEntryPointText(IProject project) {
		String entryPoint = "";
		try {
			if(project != null) {
				String property = project.getPersistentProperty(EkekoProjectPropertyPage.ENTRYPOINT_PROPERTY);
				if(property != null)
					entryPoint = property;
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
		entryPointText.setText(entryPoint);
	}

	protected void handleEntryPointButtonSelected() {
		IJavaProject project = getProjectCorrespondingToCurrentEntry();
		IType type = SootDialog.chooseMainType(getShell(), project);
		if (type != null) {
			entryPointText.setText(type.getFullyQualifiedName());
		}
	}

	private IJavaProject getProjectCorrespondingToCurrentEntry() {
		String name = projectNameText.getText().trim();
		if(name.isEmpty())
			return null;
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		IJavaProject project = JavaCore.create(root).getJavaProject(name);
		if(!project.exists())
			return null;
		return project;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		GridData gridData = new GridData();
		gridData.verticalAlignment = GridData.FILL;
		gridData.horizontalSpan = 3;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		gridData.horizontalAlignment = SWT.CENTER;

		parent.setLayoutData(gridData);
		// Create Add button
		// Own method as we need to overview the SelectionAdapter
		createOkButton(parent, OK, "Save", true);
		// Add a SelectionListener

		// Create Cancel button
		Button cancelButton = createButton(parent, CANCEL, "Cancel", false);
		// Add a SelectionListener
		cancelButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				setReturnCode(CANCEL);
				close();
			}
		});
	}

	protected Button createOkButton(Composite parent, int id, String label,boolean defaultButton) {
		// increment the number of columns in the button bar
		((GridLayout) parent.getLayout()).numColumns++;
		Button button = new Button(parent, SWT.PUSH);
		button.setText(label);
		button.setFont(JFaceResources.getDialogFont());
		button.setData(new Integer(id));
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				if (isValidInput()) {
					okPressed();
				}
			}
		});
		if (defaultButton) {
			Shell shell = parent.getShell();
			if (shell != null) {
				shell.setDefaultButton(button);
			}
		}
		setButtonLayoutData(button);
		return button;
	}

	private boolean isValidInput() {
		String projectName = projectNameText.getText().trim();
		String entryPoint = entryPointText.getText().trim();
		
		if(projectName.isEmpty() && entryPoint.isEmpty())
			return true;
		IJavaProject project =  getProjectCorrespondingToCurrentEntry();
		if (project == null) {
			setErrorMessage("Given name is not the name of a valid Java project.");
			return false;
		}
		IType main = getMainTypeCorrespondingToCurrentEntry(project);
		if (main == null) {
			setErrorMessage("Given entry point is not a valid main type in the given Java project.");
			return false;
		}
		return true;
	}
	
	private IType getMainTypeCorrespondingToCurrentEntry(IJavaProject project) {
		if (project == null)
			return null;
		String name = entryPointText.getText().trim();
		if(name.isEmpty())
			return null;
		IType type = null;
		try {
			type = project.findType(name);
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
		return type;
	}

	@Override
	protected boolean isResizable() {
		return true;
	}

	private void saveInput() {
		projectName = projectNameText.getText().trim();
		entryPoint = entryPointText.getText().trim();
		IJavaProject project = getProjectCorrespondingToCurrentEntry();
		if(project == null)
			return;
		try {
			IProject p = project.getProject();
			project.getResource().setPersistentProperty(EkekoProjectPropertyPage.ENTRYPOINT_PROPERTY, entryPoint);
			damp.util.Natures.addNature(p, SootNature.NATURE_ID);
			//build from scratch
			p.build(IncrementalProjectBuilder.FULL_BUILD, null);
		} catch (CoreException e) {
			e.printStackTrace();
		}

	}

	@Override
	protected void okPressed() {
		saveInput();
		super.okPressed();
	}

	public String getProjectName() {
		return projectName;
	}

	public String getEntryPoint() {
		return entryPoint;
	}
	
	static public IJavaProject chooseJavaProject(Shell shell, IJavaProject initialSelection) {
		IJavaProject[] projects;
		try {
			projects= JavaCore.create(ResourcesPlugin.getWorkspace().getRoot()).getJavaProjects();
		} catch (JavaModelException jme) {
			projects= new IJavaProject[0];
		}
		ILabelProvider labelProvider= new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT);
		ElementListSelectionDialog dialog= new ElementListSelectionDialog(shell, labelProvider);
		dialog.setTitle("Select a Java project"); 		
		dialog.setMessage("Select the Java project that contains an entry point for whole-program static analyses."); 
		dialog.setElements(projects);

		if (initialSelection != null) {
			dialog.setInitialSelections(new Object[] { initialSelection });
		}
		
		if (dialog.open() == ElementListSelectionDialog.OK) {			
			return (IJavaProject) dialog.getFirstResult();
		}			
		return null;		
	}
	
	public static IType chooseMainType(Shell shell, IJavaProject javaProject) {

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

		Object[] results = dialog.getResult();
		if ((results == null) || (results.length < 1)) {
			return null;
		}		
		return (IType)results[0];
			
	}
	
	
	
	
	private IProject chooseProject() {
		IProject modelProject = getModelProject();
		IJavaProject initialSelection = null;
		if(modelProject != null)
			initialSelection = JavaCore.create(modelProject);
		
		IJavaProject project = chooseJavaProject(getShell(),initialSelection);
		if(null == project)
			return null;
		return project.getProject();		
	}

	
	private static IProject getModelProject(){
		//return EkekoModel.getInstance().getWholeProgramAnalysisProject();
		return null;
	}
	

	

	
	
}


