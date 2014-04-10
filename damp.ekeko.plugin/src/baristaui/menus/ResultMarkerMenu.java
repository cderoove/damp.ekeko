package baristaui.menus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;

import baristaui.util.MarkerUtility;
import baristaui.views.queryResult.QueryView;

public class ResultMarkerMenu extends ContributionItem {

	private ITextEditor editor;
	private IVerticalRulerInfo rulerInfo;
	private List<IMarker> markers;
	
	
	

	public ResultMarkerMenu(ITextEditor editor) {
		this.editor = editor;
		this.rulerInfo = getRulerInfo();
		this.markers = getMarkers();		
	}


	@Override
	public void fill(Menu menu, int index) {
		 for (final IMarker marker : markers){
		        MenuItem menuItem = new MenuItem(menu, SWT.CASCADE | SWT.DROP_DOWN, index);
		        menuItem.setText("Ekeko Binding for:  "+marker.getAttribute(MarkerUtility.IResultMarkerAttributeBinding, "???"));
		        menuItem.setMenu(createMenusFor(marker,menu));
		    }
	}
	
	
	private Menu createMenusFor(IMarker marker, Menu parent) {
		Menu menu = new Menu(parent);
		try {
			final Map<String, Object> allBindings = (Map<String, Object>) marker
					.getAttribute(MarkerUtility.IResultMarkerAttributeOtherBindings);
			final String viewID = marker.getAttribute(
					MarkerUtility.IResultMarkerAttributeQuery, "");

			MenuItem openQuery = new MenuItem(menu, SWT.PUSH, 0);
			openQuery.setText("Open Ekeko Query Results");
			openQuery.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					try {
						IWorkbenchPage activePage = PlatformUI.getWorkbench()
								.getActiveWorkbenchWindow().getActivePage();
						IViewPart view = activePage.showView(QueryView.ID, viewID,
								IWorkbenchPage.VIEW_ACTIVATE);
					} catch (PartInitException e1) {
						//hey, if it dies ... it dies
						throw new RuntimeException(e1);
					}
				}
			});

			int index= 1;
			for(final String var:allBindings.keySet()){
				MenuItem gotoBinding = new MenuItem(menu, SWT.PUSH, index++);
				gotoBinding.setText(var);
				gotoBinding.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						
						Object o = allBindings.get(var);
						if (o instanceof IMarker) {
							IMarker marker = (IMarker) o;
							try {
								MarkerUtility.getInstance().gotoMarker(marker);
							} catch (PartInitException e1) {
								throw new RuntimeException(e1);							}
						}
						
					}
				});
			}
			
			
		} catch (CoreException e) {
			throw new RuntimeException(e);
		}
		
		
		return menu;
	}


	private IVerticalRulerInfo getRulerInfo(){
	    return (IVerticalRulerInfo) editor.getAdapter(IVerticalRulerInfo.class);
	}

	private List<IMarker> getMarkers(){
	    List<IMarker> clickedOnMarkers = new ArrayList<IMarker>();
	    try {
			for (IMarker marker : getAllMarkers()){
			    if (markerHasBeenClicked(marker)){
			        clickedOnMarkers.add(marker);
			    }
			}
		} catch (CoreException e) {
			throw new RuntimeException(e);
		}

	    return clickedOnMarkers;
	}

	//Determine whether the marker has been clicked using the ruler's mouse listener
	private boolean markerHasBeenClicked(IMarker marker){
	    return (marker.getAttribute(IMarker.LINE_NUMBER, 0)) == (rulerInfo.getLineOfLastMouseButtonActivity() + 1);
	}

	//Get all My Markers for this source file
	private IMarker[] getAllMarkers() throws CoreException{
	    return ((FileEditorInput) editor.getEditorInput()).getFile()
	        .findMarkers(MarkerUtility.IResultMarkerType, true, IResource.DEPTH_ZERO);
	}

}
