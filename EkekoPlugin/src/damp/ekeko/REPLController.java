package damp.ekeko;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.Bundle;

import ccw.util.BundleUtils;
import ccw.util.osgi.ClojureOSGi;
import ccw.util.osgi.RunnableWithException;
import clojure.lang.Keyword;
import clojure.lang.Var;
import damp.ekeko.gui.EkekoSourceProvider;

public class REPLController {

	static {
		CURRENT = new REPLController();
	}

	private REPLController() {
		clear();
	}
	
	static private REPLController CURRENT;

	static public REPLController getCurrent() {
		return CURRENT;
	}

	private ServerSocket REPLServerSocket;
	private int serverPort;
	private boolean isRunning;
	
	//starts repl in environment with correct class loader 
	private synchronized void internalStartREPLServer() {
		final Bundle ekekoBundle = Activator.getDefault().getBundle();
		ClojureOSGi.withBundle(ekekoBundle, new RunnableWithException() {
			public Object run() throws Exception {
				if (REPLServerSocket == null) {
					try {
						String ekekoBundleName = ekekoBundle.getSymbolicName();
						Var startServer = BundleUtils.requireAndGetVar(ekekoBundleName, "clojure.tools.nrepl.server/start-server");
						Object defaultHandler = BundleUtils.requireAndGetVar(ekekoBundleName, "clojure.tools.nrepl.server/default-handler").invoke();
						Object handler = BundleUtils.requireAndGetVar(ekekoBundleName, "clojure.tools.nrepl.ack/handle-ack").invoke(defaultHandler);
						REPLServerSocket = (ServerSocket)((Map)startServer.invoke(Keyword.intern("handler"), handler)).get(Keyword.intern("server-socket"));
						serverPort = REPLServerSocket.getLocalPort();
						isRunning = true;
						EkekoSourceProvider provider = EkekoSourceProvider.getSourceProvider();
						if(provider != null)
							provider.update();
						String url = String.format("nrepl://%s:%s", "localhost", getServerPort());
						Activator.log("Started Ekeko-hosted nREPL server: " + url);
					} catch (Exception e) {
						Activator.logError("Could not start Ekeko-hosted nREPL server", e);
						throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID,"Could not start plugin-hosted REPL server", e));
					}
				}
				return null;
			}});
	}

	public boolean isRunning() {
		return isRunning;
	}

	private void ensureRunning() {
		if(!isRunning()) 
			internalStartREPLServer();
	}

	public int getServerPort() {
		ensureRunning();
		return serverPort;
	}

	
	public void startREPLServer()  {
		ensureRunning();
	}

	private void clear() {
		REPLServerSocket = null;
		serverPort = 0;
		isRunning = false;
	}

	public void stopREPLServer() {
		if(!isRunning())
			return;
		try {
			REPLServerSocket.close();
			clear();
			EkekoSourceProvider provider = EkekoSourceProvider.getSourceProvider();
			if(provider != null)
				provider.update();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


}
