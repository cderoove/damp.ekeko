package damp.ekeko;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;


public class NReplCommandHandler extends AbstractHandler {
	public NReplCommandHandler() {
	}

	public Object execute(ExecutionEvent event) throws ExecutionException {		
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		try {
			String port = Integer.toString(Activator.getDefault().getREPLServerPort());
			String msg = "Ekeko-hosted nREPL running on port " + port;
			System.out.println(msg);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;

	}

}