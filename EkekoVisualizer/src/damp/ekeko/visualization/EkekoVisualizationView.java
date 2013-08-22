package damp.ekeko.visualization;


import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.zest.core.viewers.AbstractZoomableViewer;
import org.eclipse.zest.core.viewers.GraphViewer;
import org.eclipse.zest.core.viewers.IZoomableWorkbenchPart;
import org.eclipse.zest.core.widgets.Graph;


public class EkekoVisualizationView extends ViewPart implements IZoomableWorkbenchPart {

	private String viewID;

	public static final String ID = "damp.ekeko.visualization.EkekoVisualizationView";

	/*
	private Action action1;
	private Action action2;
	private Action doubleClickAction;
	*/

	private GraphViewer viewer;
	private Graph graph;
	
	public EkekoVisualizationView() {
	}

	public Graph getGraph() {
		return graph;
	}
	
	
	public void createPartControl(Composite parent) {
		//using a dummy viewer on top of the graph for the zoom contribution
	    //viewer = new GraphViewer(parent, SWT.BORDER); 
	    //graph = viewer.getGraphControl();
	    
		graph = new Graph(parent, SWT.BORDER);
		
	    /*
	    GraphNode node1 = new GraphNode(graph, SWT.NONE, "Jim");
	    GraphNode node2 = new GraphNode(graph, SWT.NONE, "Jack");
	    GraphNode node3 = new GraphNode(graph, SWT.NONE, "Joe");
	    GraphNode node4 = new GraphNode(graph, SWT.NONE, "Bill");
	    
	    new GraphConnection(graph, ZestStyles.CONNECTIONS_DIRECTED, node1, node2);
	    new GraphConnection(graph, ZestStyles.CONNECTIONS_DOT, node2, node3);
	    new GraphConnection(graph, SWT.NONE, node3, node1);
	    GraphConnection graphConnection = new GraphConnection(graph, SWT.NONE,node1, node4);
	    graphConnection.changeLineColor(parent.getDisplay().getSystemColor(SWT.COLOR_GREEN));
	    graphConnection.setText("This is a text");
	    graphConnection.setHighlightColor(parent.getDisplay().getSystemColor(SWT.COLOR_RED));
	    graphConnection.setLineWidth(3);
	    graph.setLayoutAlgorithm(new SpringLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING), true);
	    */

	    
		makeActions();
		hookContextMenu();
		hookDoubleClickAction();
		contributeToActionBars();
	}

	private void hookContextMenu() {
		/*
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				EkekoVisualizationView.this.fillContextMenu(manager);
			}
		});
		Menu menu = menuMgr.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, viewer);
		*/
	}

	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		/*
		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
		*/
	
		//ZoomContributionViewItem toolbarZoomContributionViewItem = new ZoomContributionViewItem(this);
	    //bars.getMenuManager().add(toolbarZoomContributionViewItem);

	}

	private void fillLocalPullDown(IMenuManager manager) {
		/*
		manager.add(action1);
		manager.add(new Separator());
		manager.add(action2);
		*/
	}

	private void fillContextMenu(IMenuManager manager) {
		//manager.add(action1);
		//manager.add(action2);
		// Other plug-ins can contribute there actions here
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}
	
	private void fillLocalToolBar(IToolBarManager manager) {
		/*
		manager.add(action1);
		manager.add(action2);
		*/
	}

	private void makeActions() {
		/*
		action1 = new Action() {
			public void run() {
				showMessage("Action 1 executed");
			}
		};
		action1.setText("Action 1");
		action1.setToolTipText("Action 1 tooltip");
		action1.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
			getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));
		
		action2 = new Action() {
			public void run() {
				showMessage("Action 2 executed");
			}
		};
		action2.setText("Action 2");
		action2.setToolTipText("Action 2 tooltip");
		action2.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
				getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));
		doubleClickAction = new Action() {
			public void run() {
				ISelection selection = viewer.getSelection();
				Object obj = ((IStructuredSelection)selection).getFirstElement();
				showMessage("Double-click detected on "+obj.toString());
			}
		};
		*/
	}

	private void hookDoubleClickAction() {
		/*
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				doubleClickAction.run();
			}
		});
		*/
	}
	private void showMessage(String message) {
		/*
		MessageDialog.openInformation(
			viewer.getControl().getShell(),
			"Ekeko Visualization",
			message);
		*/
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		//viewer.getControl().setFocus();
	}

	@Override
	public AbstractZoomableViewer getZoomableViewer() {
		return viewer;
	}
	
	public void setViewID(String secondaryId) {
		this.viewID = secondaryId;
	}

}