package damp.ekeko;


import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PropertyPage;

import damp.ekeko.soot.SootProjectModel;
import damp.ekeko.soot.ToggleSootNatureAction;


public class EkekoProjectPropertyPage extends PropertyPage {

	private static final String ENTRYPOINT_TITLE = "&Entry point for static analyses:";
	public static final QualifiedName ENTRYPOINT_PROPERTY = new QualifiedName(Activator.PLUGIN_ID, "ENTRYPOINT");
	private static final String DEFAULT_ENTRYPOINT = "";
	
	private static final String SOOTARGS_TITLE = "&Soot arguments:";
	private static final String SOOTARGS_TOOLTIP = "&If Ekeko Soot analyses are enabled, Soot is executed with these arguments whenever the project is built.";
	public static final QualifiedName SOOTARGS_PROPERTY = new QualifiedName(Activator.PLUGIN_ID, "SOOTARGS");
	private static final String DEFAULT_SOOTARGS = SootProjectModel.DEFAULT_SOOTARGS;

	private static final int TEXT_FIELD_WIDTH = 50;

	private Text entryPointText;
	private Text sootArgsText;

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

		// Owner text field
		entryPointText = new Text(composite, SWT.SINGLE | SWT.BORDER);
		GridData gd = new GridData();
		gd.widthHint = convertWidthInCharsToPixels(TEXT_FIELD_WIDTH);
		entryPointText.setLayoutData(gd);

		Button selectButton = new Button(composite, SWT.PUSH);
		selectButton.setText("Select...");
		
		selectButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleSearchButtonSelected();
			}});
		
		GridData buttonGD = new GridData();
		selectButton.setLayoutData(buttonGD);
		
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

		sootArgsText = new Text(composite, SWT.MULTI | SWT.BORDER);
		GridData gd = new GridData();
		gd.widthHint = convertWidthInCharsToPixels(TEXT_FIELD_WIDTH);
		gd.heightHint = sootArgsText.getLineHeight()*2;
		sootArgsText.setLayoutData(gd);
		
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
		GridLayout layout = new GridLayout();
		composite.setLayout(layout);
		GridData data = new GridData(GridData.FILL);
		data.grabExcessHorizontalSpace = true;
		composite.setLayoutData(data);

		addEntryPointTextBox(composite);
		addSeparator(composite, 20);
		addSootArgsTextBox(composite);
		
		return composite;
	}

	private Composite createDefaultComposite(Composite parent) {
		Composite composite = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		composite.setLayout(layout);

		GridData data = new GridData();
		data.verticalAlignment = GridData.FILL;
		data.horizontalAlignment = GridData.FILL;
		composite.setLayoutData(data);

		return composite;
	}

	protected void performDefaults() {
		super.performDefaults();
		// Populate the owner text field with the default value
		entryPointText.setText(DEFAULT_ENTRYPOINT);
		sootArgsText.setText(DEFAULT_SOOTARGS);
	}

	public boolean performOk() {
		// store the value in the owner text field
		try {
			getResource().setPersistentProperty(ENTRYPOINT_PROPERTY, entryPointText.getText());
			getResource().setPersistentProperty(SOOTARGS_PROPERTY, sootArgsText.getText());
		} catch (CoreException e) {
			return false;
		}
		return true;
	}
	
	public void addSeparator(Composite parent, int horizontalSpan) {
		addLabel(parent, horizontalSpan, SWT.SEPARATOR | SWT.HORIZONTAL);
	}
	
	public Label addLabel(Composite parent, int horizontalSpan, int style) {
		Label label = new Label(parent, style);
		GridData gd = new GridData();
		gd.horizontalSpan = horizontalSpan;
		gd.horizontalAlignment = GridData.FILL;
		gd.grabExcessHorizontalSpace = true;
		label.setLayoutData(gd);

		return label;
	}

}