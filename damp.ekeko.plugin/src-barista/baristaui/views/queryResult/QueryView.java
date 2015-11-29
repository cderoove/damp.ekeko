package baristaui.views.queryResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ExpandBar;
import org.eclipse.swt.widgets.ExpandItem;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.menus.IMenuService;
import org.eclipse.ui.part.ViewPart;

import baristaui.util.ExpandBarFixListener;
import baristaui.util.MarkerUtility;
import baristaui.views.queryResult.column.ColumnSelectionListener;
import baristaui.views.queryResult.table.TableViewerConfigurator;
import baristaui.views.queryResult.tree.SOULTreeLabelProvider;
import baristaui.views.queryResult.tree.TreeResultContentProvider;
import baristaui.views.queryResult.tree.TreeViewerConfigurator;
import baristaui.views.queryResult.variableTable.VariableTableConfigurator;
import damp.ekeko.EkekoPlugin;

public class QueryView extends ViewPart {

	public static String ID = "ekeko.BaristaUI.queryResults";

	// private IEvaluator evaluator;
	private TableViewer tableViewer;
	private TableViewerConfigurator tableConf;  
	private TabItem tableResultViewTab;
	private TabItem columnResultViewTab;
	private TabFolder views;

	private Label totalResults;

	private Label timeElapsed;

	private String workingSetName;

	private String query;

	private Text queryTxt;

	private TabItem treeResultView;


	private Button nextResult;

	private Composite varsComposite;

	private TableViewer variableTable;

	protected Map<String, List<Object>> currentResults;

	private Button refreshVariablesButton;

	private IEditorInput originatingInput;

	private Button allResults;

	private MarkerUtility.Handle currentMarkers;

	private String viewID;

	private LabelProvider provider;
	
	private TreeViewerConfigurator conf;
	private Composite main_1;


	public String getViewID() {
		return viewID;
	}

	public QueryView() {
		provider = new SOULLabelProvider();
		conf = new TreeViewerConfigurator(provider);
		tableConf = new TableViewerConfigurator(provider);
	}
	
	public void setLabelProvider(LabelProvider p) {
		provider = p;
		conf = new TreeViewerConfigurator(provider);
		tableConf = new TableViewerConfigurator(provider);
	}
	
	/*
	public void updateLabelProvider(SOULLabelProvider p) {
		provider = p;
		String elapsed = timeElapsed.getText();
		updateResultViews(currentResults, Integer.valueOf(elapsed.substring(0, elapsed.length() - 3)),  Integer.valueOf(totalResults.getText()));
	}
	*/

	public void updateResultViews(Map<String, List<Object>> resultmap, long elapsed, long total) {
		totalResults.setText("" + total);
		timeElapsed.setText(elapsed + " ms");
		timeElapsed.redraw();
		timeElapsed.getParent().layout(true);
		currentResults = resultmap;
		updateTableViewWith(resultmap);
		updateTreeViewWith(resultmap);
		updateColumnViewWith(resultmap);
		/*
		 * if (!evaluator.hasMoreResults()) { nextResult.setEnabled(false); }
		 */
		refreshVariablesButton.setEnabled(true);
	}

	public TabItem createColumnItem(TabFolder views) {
		TabItem columnResultViewTab = new TabItem(views, SWT.NONE);
		columnResultViewTab.setText("Columns");
		return columnResultViewTab;
	}

	@Override
	public void createPartControl(Composite parent) {

		main_1 = new Composite(parent, SWT.NONE);
		GridLayout gl_main_1 = new GridLayout(2, false);
		main_1.setLayout(gl_main_1);

		createVariablesConfig(main_1);

		Composite buttons = new Composite(main_1, SWT.NONE);
		buttons.setLayout(new GridLayout(2, true));
		/*
		 * allResults = new Button(buttons, SWT.NONE); GridData gd = new
		 * GridData(SWT.TOP, SWT.LEAD, false, false);
		 * allResults.setLayoutData(gd); allResults.setText("All Results");
		 * 
		 * nextResult = new Button(buttons, SWT.NONE); gd = new
		 * GridData(SWT.TOP, SWT.LEAD, true, false);
		 * nextResult.setLayoutData(gd); nextResult.setText("Next Result");
		 * 
		 * allResults.addSelectionListener(new SelectionAdapter() { public void
		 * widgetSelected(SelectionEvent event) { IResults results =
		 * evaluator.getAllResults(); updateResultViews(results); }
		 * 
		 * });
		 * 
		 * nextResult.addSelectionListener(new SelectionAdapter() { public void
		 * widgetSelected(SelectionEvent event) { IResults results =
		 * evaluator.getNextResult(); updateResultViews(results); } });
		 */
		Button markResultsButton = new Button(buttons, SWT.TOGGLE);
		markResultsButton.setText("Mark Results");
		new Label(buttons, SWT.NONE);
		markResultsButton.addSelectionListener(new SelectionAdapter() {
			private boolean marking = false;

			@Override
			public void widgetSelected(SelectionEvent e) {
				marking = !marking;
				markResults(marking);
			}
		});

		createStatsBar(main_1);

		views = new TabFolder(main_1, SWT.NONE);
		views.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		tableResultViewTab = createTableItem(views);
		columnResultViewTab = createColumnItem(views);
		treeResultView = createTreeItem(views);
		new Label(main_1, SWT.NONE);

	}

	protected void markResults(boolean marking) {
		try {
			// String projectName = this.getProject();

			if (!marking) {
				removeMyMarkers();
			} else {
				createMarkersFor(currentResults);
			}
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void createMarkersFor(Map<String, List<Object>> results) throws CoreException {

		currentMarkers = MarkerUtility.getInstance().createHandlesFor(results, getActiveVariables(results), getViewID());
	}

	@Override
	public void dispose() {
		try {
			MarkerUtility.getInstance().removeMarkersFor(currentMarkers);
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		super.dispose();
	}

	private void removeMyMarkers() throws CoreException {
		MarkerUtility.getInstance().removeMarkersFor(currentMarkers);
	}

	private void createStatsBar(Composite main) {
		new Label(main_1, SWT.NONE);
		ExpandBar queryStatsBar = new ExpandBar(main, SWT.NONE);
		GridData layoutData = new GridData(SWT.FILL, SWT.FILL, true, false);
		layoutData.horizontalSpan = 2;
		queryStatsBar.setLayoutData(layoutData);

		Composite statsC = new Composite(queryStatsBar, SWT.NONE);
		GridData layoutData2 = new GridData(SWT.LEAD, SWT.FILL, false, true);
		layoutData2.widthHint = 60;
		statsC.setLayoutData(layoutData2);
		GridLayout statsLayout = new GridLayout(2, false);
		statsC.setLayout(statsLayout);

		Label totalResulsLabel = new Label(statsC, SWT.NONE);
		totalResulsLabel.setText("Total results:");
		totalResults = new Label(statsC, SWT.None);

		Label timeElapsedLabel = new Label(statsC, SWT.NONE);
		timeElapsedLabel.setText("Time elapsed:");
		timeElapsed = new Label(statsC, SWT.NONE);

		Label queryLabel = new Label(statsC, SWT.NONE);
		queryLabel.setText("Original Query:");
		new Label(statsC, SWT.NONE);
		queryTxt = new Text(statsC, SWT.BORDER_DASH | SWT.MULTI | SWT.READ_ONLY | SWT.V_SCROLL | SWT.H_SCROLL);

		GridData txtGD = new GridData(SWT.FILL, SWT.FILL, true, false);
		txtGD.horizontalSpan = 2;
		txtGD.widthHint = 70;
		txtGD.heightHint = 50;
		txtGD.grabExcessHorizontalSpace = true;
		queryTxt.setLayoutData(txtGD);

		/*
		 * Button openOriginalEditor = new Button(statsC, SWT.NONE);
		 * openOriginalEditor.setText("open query");
		 * openOriginalEditor.addSelectionListener(new SelectionAdapter() {
		 * 
		 * @Override public void widgetSelected(SelectionEvent e) {
		 * IWorkbenchPage activePage = PlatformUI.getWorkbench()
		 * .getActiveWorkbenchWindow().getActivePage(); try {
		 * activePage.openEditor(originatingInput, QueryEditor.ID); } catch
		 * (PartInitException e1) { throw new RuntimeException(e1); } } });
		 */

		ExpandItem queryStats = new ExpandItem(queryStatsBar, SWT.NONE, 0);
		queryStats.setText("Query Stats");
		queryStats.setHeight(statsC.computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
		queryStats.setControl(statsC);
		queryStats.setExpanded(false);
		queryStatsBar.addExpandListener(new ExpandBarFixListener(queryStatsBar));
	}

	public TabItem createTableItem(TabFolder views) {
		TabItem tableResultViewTab = new TabItem(views, SWT.NONE);
		tableResultViewTab.setText("Table");

		return tableResultViewTab;
	}

	public TabItem createTreeItem(TabFolder views) {
		TabItem treeResultViewTab = new TabItem(views, SWT.NONE);
		treeResultViewTab.setText("Tree");
		return treeResultViewTab;
	}

	private void createVariablesConfig(Composite main) {
		ExpandBar variableBar = new ExpandBar(main, SWT.NONE);
		GridData layoutData = new GridData(SWT.FILL, SWT.FILL, true, false);
		layoutData.horizontalSpan = 2;
		variableBar.setLayoutData(layoutData);

		varsComposite = new Composite(variableBar, SWT.NONE);
		GridLayout statsLayout = new GridLayout(2, true);
		varsComposite.setLayout(statsLayout);

		variableTable = new TableViewer(varsComposite, SWT.V_SCROLL | SWT.BORDER | SWT.MULTI);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.widthHint = 30;
		gd.heightHint = 80;
		gd.verticalSpan = 4;
		variableTable.getTable().setLayoutData(gd);

		final Button moveVarUp = new Button(varsComposite, SWT.NONE);
		moveVarUp.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				IStructuredSelection selection = (IStructuredSelection) variableTable.getSelection();
				Object selected = selection.getFirstElement();
				moveVarUpInTable(selected, true);
			}
		});
		moveVarUp.setToolTipText("move variable up");
		// XXX
		moveVarUp.setImage(EkekoPlugin.getImage("icons/go-up.png"));
		moveVarUp.setEnabled(false);

		refreshVariablesButton = new Button(varsComposite, SWT.NONE);
		refreshVariablesButton.setToolTipText("refresh query results");
		refreshVariablesButton.setImage(EkekoPlugin.getImage("icons/view-refresh.png"));
		refreshVariablesButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateTableViewWith(currentResults);
				updateTreeViewWith(currentResults);
				updateColumnViewWith(currentResults);
			}
		});
		refreshVariablesButton.setEnabled(currentResults != null); // cannot
																	// refresh
																	// -- no
																	// results!

		Button resetVariablesButton = new Button(varsComposite, SWT.PUSH);
		resetVariablesButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
		resetVariablesButton.setToolTipText("reset variables");
		resetVariablesButton.setImage(EkekoPlugin.getImage("icons/edit-clear.png"));
		resetVariablesButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				variableTable.setSelection(StructuredSelection.EMPTY);
				variableTable.setInput(getActiveVariables(currentResults));
			}
		});

		final Button moveVarDown = new Button(varsComposite, SWT.NONE);
		moveVarDown.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				IStructuredSelection selection = (IStructuredSelection) variableTable.getSelection();
				Object selected = selection.getFirstElement();
				moveVarUpInTable(selected, false);
			}
		});
		moveVarDown.setImage(EkekoPlugin.getImage("icons/go-down.png"));
		moveVarDown.setEnabled(false);

		variableTable.addSelectionChangedListener(new ISelectionChangedListener() {

			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				ISelection s = event.getSelection();
				if (s instanceof IStructuredSelection) {
					IStructuredSelection selection = (IStructuredSelection) s;
					moveVarUp.setEnabled(selection.size() == 1);
					moveVarDown.setEnabled(selection.size() == 1);
				}
			}
		});

		ExpandItem queryStats = new ExpandItem(variableBar, SWT.NONE, 0);
		queryStats.setText("Query Variables");
		queryStats.setHeight(varsComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
		queryStats.setControl(varsComposite);
		queryStats.setExpanded(false);
		variableBar.addExpandListener(new ExpandBarFixListener(variableBar));

	}

	private String[] getActiveVariables(Map<String, List<Object>> results) {

		VariableTableConfigurator vtconf = new VariableTableConfigurator(variableTable);

		ISelection selectedVars = variableTable.getSelection();

		if (selectedVars instanceof IStructuredSelection) {
			IStructuredSelection selection = (IStructuredSelection) selectedVars;

			if (selection.isEmpty()) {
				Set keySet = results.keySet();
				String[] variables = (String[]) keySet.toArray(new String[keySet.size()]); // by
																							// default
																							// return
																							// all
																							// variables
				vtconf.configureFor(results, variables);
				return variables;
			}

			List<String> vars = new ArrayList<String>(selection.size());
			for (int i = 0; i < selection.toArray().length; i++) {
				Object o = selection.toArray()[i];
				if (o instanceof String) {
					String s = (String) o;
					vars.add(s);
				}
			}

			String[] variables = (String[]) vars.toArray(new String[vars.size()]);
			vtconf.configureFor(results, variables);
			return variables;
		}

		return new String[] { "Error?" };

	}

	protected void moveVarUpInTable(Object selected, boolean up) {
		if (selected instanceof String) {
			String var = (String) selected;
			String[] vars = (String[]) variableTable.getInput();

			for (int i = 0; i < vars.length; i++) {
				if (var.equals(vars[i])) {
					if (up && i > 0) {
						String temp = vars[i - 1];
						vars[i - 1] = var;
						vars[i] = temp;
					}
					if (!up && i < vars.length - 1) {
						String temp = vars[i + 1];
						vars[i + 1] = var;
						vars[i] = temp;
					}
				}
			}

			variableTable.setInput(vars);
			variableTable.getTable().layout();

		}

	}

	@Override
	public void setFocus() {

	}

	public void setOriginatingEditorInput(IEditorInput editorInput) {
		this.originatingInput = editorInput;
	}

	public void setQuery(String q) {
		query = q;
		queryTxt.setText(query);
		queryTxt.getParent().layout();
	}

	private void updateColumnViewWith(Map<String, List<Object>> results) {
		String[] variables = getActiveVariables(results);

		Composite columnView = new Composite(views, SWT.NONE);
		columnView.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		columnView.setLayout(new GridLayout(variables.length, true));
		TableViewer[] columns = new TableViewer[variables.length];
		TreeResultContentProvider trcp = new TreeResultContentProvider(variables);
		SOULTreeLabelProvider labelProvider = new SOULTreeLabelProvider(provider);
		IContentProvider provider = new ArrayContentProvider();
		GridData columnLayout = new GridData(SWT.FILL, SWT.FILL, true, true);
		// columnLayout.widthHint = 200;

		for (int i = 0; i < variables.length; i++) {
			columns[i] = new TableViewer(columnView, SWT.V_SCROLL | SWT.BORDER);
			columns[i].getTable().setLayoutData(columnLayout);
			columns[i].setContentProvider(provider);
			columns[i].setLabelProvider(labelProvider);
			columns[i].addDoubleClickListener(new ColumnSelectionListener(columns, i, trcp));
			
			final MenuManager menuMgr = new MenuManager();
			menuMgr.setRemoveAllWhenShown(true);
			IMenuService menuService = (IMenuService) PlatformUI.getWorkbench().getService(IMenuService.class);
			menuService.populateContributionManager(menuMgr, "popup:BaristaUI.TreeResultsMenu");
			columns[i].getControl().setMenu(menuMgr.createContextMenu(columns[i].getControl()));
			getSite().registerContextMenu(menuMgr, columns[i]);

		}
		if (variables.length != 0) {
			columns[0].setInput(trcp.getElements(results));
		}

		columnResultViewTab.setControl(columnView);
	}

	private void updateTableViewWith(Map<String, List<Object>> results) {

		String[] variables = getActiveVariables(results);

		tableViewer = new TableViewer(views, SWT.H_SCROLL | SWT.V_SCROLL | SWT.HIDE_SELECTION | SWT.BORDER);
		
		Table table = tableViewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		tableResultViewTab.setControl(table);
		tableConf.setViewer(tableViewer);

		tableConf.configureFor(results, variables);
		
		addActiveColumnListener(table);
		addMenu(tableViewer);
	}
	
	// TODO START -- Copy-pasted code from RecommendationEditor in Ekeko/X ; should be refactored?
	private int activeColumn = -1;
	
	private void addActiveColumnListener(final Table table) {
		table.addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent e) {
				int x = 0;
				for (int i = 0; i < table.getColumnCount(); i++) {
					x +=table.getColumn(i).getWidth();
					if (e.x <= x) {
						activeColumn = i;
						break;
					}
				}
			}
		});
	}
	
	private void addMenu(final TableViewer tableViewer) {
		final Table table = tableViewer.getTable();
		final MenuManager mgr = new MenuManager();

		final Action revealNode = new Action("Reveal In Editor") {
			public void run() {
				revealNode(tableViewer, activeColumn);
			}
		};

		mgr.setRemoveAllWhenShown(true);
		mgr.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(IMenuManager manager) {
				manager.add(revealNode);
			}
		});

		table.setMenu(mgr.createContextMenu(table));
	}
	
	protected void revealNode(TableViewer viewer, int activeColumn) {
		ISelection selection = viewer.getSelection();
		IStructuredSelection sel = (IStructuredSelection) selection;
		if(sel.isEmpty()) 
			return;
		Collection tuple = ((HashMap) sel.getFirstElement()).values();
		Object element = nth(tuple, activeColumn);
		if(element instanceof ASTNode) {
			ASTNode astNode = (ASTNode) element;
			MarkerUtility.getInstance().createMarkerAndGoto(astNode);
		}
	}
	
	private Object nth(Collection coll, int n) {
		Iterator iterator = coll.iterator();
		for(int i=0; i<n; i++){
			iterator.next();
		}
		return iterator.next();
	}
	// TODO - END Copy-pasted code

	protected void updateTreeViewWith(Map<String, List<Object>> results) {
		TreeViewer treeViewer = new TreeViewer(views, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL);

		String[] variables = getActiveVariables(results);

		conf.setViewer(treeViewer);

		Tree tree = treeViewer.getTree();

		treeResultView.setControl(tree);

		final MenuManager menuMgr = new MenuManager();
		menuMgr.setRemoveAllWhenShown(true);
		IMenuService menuService = (IMenuService) PlatformUI.getWorkbench().getService(IMenuService.class);
		menuService.populateContributionManager(menuMgr, "popup:BaristaUI.TreeResultsMenu");
		treeResultView.getControl().setMenu(menuMgr.createContextMenu(treeResultView.getControl()));

		getSite().registerContextMenu(menuMgr, treeViewer);

		conf.configureFor(results, variables);
		tree.layout();
	}

	public void setWorkingSetName(String wsName) {
		this.workingSetName = wsName;
	}

	public String getWorkingSetName() {
		return workingSetName;
	}

	public void setViewID(String secondaryId) {
		this.viewID = secondaryId;
	}

}
