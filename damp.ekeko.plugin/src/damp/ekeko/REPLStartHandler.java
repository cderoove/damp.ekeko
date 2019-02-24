package damp.ekeko;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;


public class REPLStartHandler extends AbstractHandler {
	
	public REPLStartHandler() {
	}

	public Object execute(ExecutionEvent event) throws ExecutionException {		
		try {
			REPLController.getCurrent().startREPLServer();
			Shell shell = HandlerUtil.getActiveWorkbenchWindowChecked(event).getShell();
			MessageDialog.openInformation(shell, 
					"Ekeko-hosted nREPL server started", 
					"Successfully started an Ekeko-hosted nREPL server on port " + REPLController.getCurrent().getServerPort() + ".\n" +
					"Connect to this read-eval-print loop using any of the nREPL clients listed at https://nrepl.org/.");		
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;

	}

}