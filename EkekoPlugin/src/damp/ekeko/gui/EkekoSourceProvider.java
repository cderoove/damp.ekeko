package damp.ekeko.gui;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.ui.AbstractSourceProvider;
import org.eclipse.ui.ISources;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.services.ISourceProviderService;

import damp.ekeko.REPLController;

public class EkekoSourceProvider extends AbstractSourceProvider {

	@Override
	public void dispose() {
	}

	@Override
	public Map<String, Object> getCurrentState() {
		Map<String, Object> stateMap = new HashMap<String, Object>(1);	
		stateMap.put(REPL_RUNNING, new Boolean(REPLController.getCurrent().isRunning()));
		System.out.println(stateMap.toString());
		return stateMap;
	}

	public final static String REPL_RUNNING = "damp.ekeko.services.REPLRunning";

	@Override
	public String[] getProvidedSourceNames() {
		return new String[] { REPL_RUNNING };
	}
	
	public static EkekoSourceProvider getSourceProvider() {
        ISourceProviderService service = (ISourceProviderService) PlatformUI.getWorkbench().getService(ISourceProviderService.class);  
        if(service == null)
        	return null;
        return (EkekoSourceProvider) service.getSourceProvider(REPL_RUNNING);  
	}
	
	public void update() {
		fireSourceChanged(ISources.WORKBENCH, getCurrentState());  
	}
	
}
