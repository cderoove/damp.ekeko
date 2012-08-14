package baristaui.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.ui.IMarkerActionFilter;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

public class MarkerUtility {
	
	public static class Handle{};
	
	static private MarkerUtility instance;

	public static final String IResultMarkerType = "BaristaUI.EkekoResultBinding";

	public static final String IResultMarkerAttributeQuery = "BaristaUI.Query";
	
	public static final String IResultMarkerAttributeOtherBindings = "BaristaUI.OtherBindings";
	
	public static final String IResultMarkerAttributeBinding = "BaristaUI.Binding";
	
	
	Map<Handle,List<IMarker>> registry;
	
	
	private MarkerUtility(){
		registry = new HashMap<Handle, List<IMarker>>();
	}

	
	public static MarkerUtility getInstance() {
		if (instance == null) {
			instance = new MarkerUtility();
		}

		return instance;
	}
	
	public Handle createMarker(ASTNode node) throws CoreException{
		Handle handle= new Handle();
		
		ASTNode root = node.getRoot();
		
		if (root instanceof CompilationUnit) {
			CompilationUnit cu = (CompilationUnit) root;
			int nodeLineNum = cu.getLineNumber(node.getStartPosition());
			IMarker newMarker = cu.getJavaElement().getCorrespondingResource().createMarker(IResultMarkerType);
			
			newMarker.setAttribute(IMarker.LINE_NUMBER, nodeLineNum);
			newMarker.setAttribute(IMarker.CHAR_START,node.getStartPosition());
			newMarker.setAttribute(IMarker.CHAR_END, node.getStartPosition() + node.getLength());
			
			registerNewMarker(handle,newMarker);
			
		}
		
		
		return handle;
	}


	private void registerNewMarker(Handle handle, IMarker newMarker) {
		if(registry.get(handle) == null){
			registry.put(handle, new  ArrayList<IMarker>());
		}
		registry.get(handle).add(newMarker);
	}
	
	public void removeMarkersFor(Handle handle) throws CoreException{
		List<IMarker> markers = registry.get(handle);
		
		if(markers == null) return;

		for (Iterator iterator = markers.iterator(); iterator.hasNext();) {
			IMarker iMarker = (IMarker) iterator.next();		
			if(iMarker.exists()){
				iMarker.delete();
			}
		}
		
		registry.remove(handle);
		
	}

	
	public void createMarkerAndGoto(ASTNode node){
		try {
			Handle h = createMarker(node);
			IMarker marker = registry.get(h).get(0);
			gotoMarker(marker);
			removeMarkersFor(h);
		} catch (PartInitException e) {
			e.printStackTrace();
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}


	public void gotoMarker(IMarker marker) throws PartInitException {
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		IDE.openEditor(page, marker);
	}
	
	public Handle createHandlesFor(Map<String,List<Object>> results, String[] variables, String viewID) throws  CoreException{
		Handle h = new Handle();
		
		if(variables.length == 0) return null; //no results
		int resultSize = results.get(variables[0]).size();
		
		for(int i = 0; i<resultSize;i++){
			Map<String,Object> record = new HashMap<String, Object>();
			Object[] bindingObjects = new Object[variables.length];
			for(int j=0; j<variables.length;j++){
				
				String var = variables[j];
				Object binding = results.get(variables[j]).get(i);
				
				if (binding instanceof ASTNode) {
					ASTNode node = (ASTNode) binding;
					ASTNode root = node.getRoot();
					
					if (root instanceof CompilationUnit) {
						CompilationUnit cu = (CompilationUnit) root;
						int nodeLineNum = cu.getLineNumber(node.getStartPosition());
						IMarker newMarker = cu.getJavaElement().getCorrespondingResource().createMarker(IResultMarkerType);
						
						newMarker.setAttribute(IMarker.LINE_NUMBER, nodeLineNum);
						newMarker.setAttribute(IMarker.CHAR_START,node.getStartPosition());
						newMarker.setAttribute(IMarker.CHAR_END, node.getStartPosition() + node.getLength());
						newMarker.setAttribute(IMarker.MESSAGE, var);
						//TODO: add attribs with the variable and other stuff
						
						newMarker.setAttribute(IResultMarkerAttributeBinding, var);
						newMarker.setAttribute(IResultMarkerAttributeQuery, viewID);
						
						record.put(var, newMarker);
						registerNewMarker(h, newMarker);
						bindingObjects[j] = newMarker;
					}
					
				}else{
					record.put(var, binding);
					bindingObjects[j] = binding;
				}
				
			}
			
			for (int j = 0; j < bindingObjects.length; j++) {
				Object o = bindingObjects[j];
				if (o instanceof IMarker) {
					IMarker m = (IMarker) o;
					m.setAttribute(IResultMarkerAttributeOtherBindings, record);
				}
				
			}
			
			
			
			
		}
		
	
		
		return h;
	}
	
	
}
