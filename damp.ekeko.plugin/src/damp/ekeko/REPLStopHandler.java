package damp.ekeko;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;


public class REPLStopHandler extends AbstractHandler {
	
	public REPLStopHandler() {
	}

	public Object execute(ExecutionEvent event) throws ExecutionException {		
		try {
			REPLController.getCurrent().stopREPLServer();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;

	}

}