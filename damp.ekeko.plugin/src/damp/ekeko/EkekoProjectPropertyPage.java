package damp.ekeko;


import java.awt.Checkbox;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PropertyPage;

import damp.ekeko.soot.SootProjectModel;
import damp.ekeko.soot.ToggleSootNatureAction;


public class EkekoProjectPropertyPage extends PropertyPage {

	private static final String ENTRYPOINT_TITLE = "&Entry point for static analyses:";
	public static final QualifiedName ENTRYPOINT_PROPERTY = new QualifiedName(EkekoPlugin.PLUGIN_ID, "ENTRYPOINT");
	private static final String DEFAULT_ENTRYPOINT = "";
	
	private static final String SOOTARGS_TITLE = "&Soot arguments:";
	private static final String SOOTARGS_TOOLTIP = "&If Ekeko Soot analyses are enabled, Soot is executed with these arguments whenever the project is built.";
	public static final QualifiedName SOOTARGS_PROPERTY = new QualifiedName(EkekoPlugin.PLUGIN_ID, "SOOTARGS");
	private static final String DEFAULT_SOOTARGS = SootProjectModel.DEFAULT_SOOTARGS;

		
	public static final QualifiedName PROCESSERRORS_PROPERTY = new QualifiedName(EkekoPlugin.PLUGIN_ID, "PROCESSERRORS");

	
	private static final int TEXT_FIELD_WIDTH = 50;

	private Text entryPointText;
	private Text sootArgsText;
	private Button checkIgnoreError;

	private IResource getResource() {
		IAdaptable e = getElement();
		if (e instanceof IJavaProject) {
			return ((IJavaProject) e).getResource();
		} else {
			return (IResource) e;
		}
	}
	
	private IJavaProject getJavaProject() {
		IAdaptable e = getElement();
		if (e instanceof IJavaProject) {
			return (IJavaProject) e;
		} else 
			return JavaCore.create((IProject) e); 
	}
	
	private void handleSearchButtonSelected() {
		IType type = ToggleSootNatureAction.chooseEntryPoint(getShell(), getJavaProject());
		if (type != null) {
			entryPointText.setText(type.getFullyQualifiedName());
		}
	}

	private void addEntryPointTextBox(Composite parent) {
		Composite composite = createDefaultComposite(parent);

		// Label for owner field
		Label ownerLabel = new Label(composite, SWT.NONE);
		ownerLabel.setText(ENTRYPOINT_TITLE);
		
		GridData ownerLabelGD = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		ownerLabel.setLayoutData(ownerLabelGD);
		

		// Owner text field
		entryPointText = new Text(composite, SWT.BORDER);
		GridData entryPointGD = new GridData(SWT.FILL, SWT.CENTER, true, false);
		

		//entryPointGD.widthHint = convertWidthInCharsToPixels(TEXT_FIELD_WIDTH);
		
		entryPointText.setLayoutData(entryPointGD);

		Button selectButton = new Button(composite, SWT.PUSH);
		GridData buttonGD = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
		
		selectButton.setLayoutData(buttonGD);
		selectButton.setText("Select...");
		
	
		selectButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleSearchButtonSelected();
			}});
		
		
		// Populate owner text field
		try {
			String owner = getResource().getPersistentProperty(ENTRYPOINT_PROPERTY);
			entryPointText.setText((owner != null) ? owner : DEFAULT_ENTRYPOINT);
		} catch (CoreException e) {
			entryPointText.setText(DEFAULT_ENTRYPOINT);
		}
	}
	
	private void addSootArgsTextBox(Composite parent) {
		Composite composite = createDefaultComposite(parent);

		Label ownerLabel = new Label(composite, SWT.NONE);
		ownerLabel.setText(SOOTARGS_TITLE);
		GridData ownerLabelGD = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		ownerLabel.setLayoutData(ownerLabelGD);

		sootArgsText = new Text(composite, SWT.BORDER);
		GridData sootArgsTextGD = new GridData(SWT.FILL, SWT.CENTER, true, false);
		sootArgsTextGD.horizontalSpan = 2;
		sootArgsText.setLayoutData(sootArgsTextGD);

		
		//gd.widthHint = convertWidthInCharsToPixels(TEXT_FIELD_WIDTH);
		//gd.heightHint = sootArgsText.getLineHeight()*2;
		
		try {
			String owner = getResource().getPersistentProperty(SOOTARGS_PROPERTY);
			sootArgsText.setText((owner != null) ? owner : DEFAULT_SOOTARGS);
			sootArgsText.setToolTipText(SOOTARGS_TOOLTIP);
		} catch (CoreException e) {
			sootArgsText.setText(DEFAULT_SOOTARGS);
		}
	}

	/**
	 * @see PreferencePage#createContents(Composite)
	 */
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, true));

		RowLayout layout = new RowLayout(SWT.VERTICAL);
		layout.fill = true;
		//layout.pack = false;
		composite.setLayout(layout);
		
		Composite jdtGroup = createGroup(composite, "Structural information obtained from JDT");
		addIgnoreErrors(jdtGroup);
		
		
		
		Composite sootGroup = createGroup(composite, "Behavioral information obtained from SOOT");
		
		addEntryPointTextBox(sootGroup);
		addSootArgsTextBox(sootGroup);
		

		
		//addSeparator(composite, 20);
		
		return composite;
	}
	
	private void addIgnoreErrors(Composite jdtGroup) {
		checkIgnoreError = new Button(jdtGroup, SWT.CHECK);
		checkIgnoreError.setText("Include files with compilation errors in queries (not recommended)");
		checkIgnoreError.setSelection(false);
		try {
			String errors = getResource().getPersistentProperty(PROCESSERRORS_PROPERTY);
			checkIgnoreError.setSelection(Boolean.parseBoolean(errors));
		} catch (CoreException e) {
			checkIgnoreError.setSelection(false);
		}
	}

	private Composite createGroup(Composite parent, String text) {
		Group group = new Group(parent, SWT.DEFAULT);
		group.setText(text);
		
		//Composite composite = new Composite(group, SWT.NONE);
		GridLayout layout = new GridLayout(3,false);
		group.setLayout(layout);
		
		//composite.setLayoutData(data);

		return group;
	}
	

	private Composite createDefaultComposite(Composite parent) {
		/*
		Composite composite = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		composite.setLayout(layout);

		GridData data = new GridData();
		data.verticalAlignment = GridData.FILL;
		data.horizontalAlignment = GridData.FILL;
		composite.setLayoutData(data);
		 */
		return parent;
	}

	protected void performDefaults() {
		super.performDefaults();
		// Populate the owner text field with the default value
		entryPointText.setText(DEFAULT_ENTRYPOINT);
		sootArgsText.setText(DEFAULT_SOOTARGS);
		checkIgnoreError.setSelection(false);
	}

	public boolean performOk() {
		// store the value in the owner text field
		try {
			getResource().setPersistentProperty(ENTRYPOINT_PROPERTY, entryPointText.getText());
			getResource().setPersistentProperty(SOOTARGS_PROPERTY, sootArgsText.getText());
			getResource().setPersistentProperty(PROCESSERRORS_PROPERTY, Boolean.toString(checkIgnoreError.getSelection()));			
			for(IProjectModel pm : EkekoModel.getInstance().getProjectModel(getResource())) {
				pm.clean();
				pm.populate(new NullProgressMonitor());
			}
		} catch (CoreException e) {
			return false;
		}
		return true;
	}
	
	

}