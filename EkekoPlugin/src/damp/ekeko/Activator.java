package damp.ekeko;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import ccw.repl.REPLView;
import ccw.util.BundleUtils;
import clojure.lang.Agent;
import clojure.lang.Keyword;
import clojure.lang.Var;
import clojure.osgi.ClojureOSGi;
import clojure.osgi.RunnableWithException;

public class Activator extends AbstractUIPlugin {

	public static final String PLUGIN_ID = "damp.ekeko.plugin"; //$NON-NLS-1$
	private static Activator plugin;

	private static Map<String, Image> pluginImages = new HashMap<String, Image>();

	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		EkekoModel.registerDefaultFactories();
		this.registerContributedFactories();
		
		EkekoModel.getInstance().populate();
		startClojureCode(context);
	}

	public void stop(BundleContext context) throws Exception {
		stopREPLServer();
		plugin = null;
		super.stop(context);
	}
	
	public static Activator getDefault() {
		return plugin;
	}

	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}

	private void startClojureCode(BundleContext bundleContext) throws Exception {
		ClojureOSGi.require(bundleContext, "clojure.stacktrace"); 
		ClojureOSGi.require(bundleContext, "clojure.test");  
		ClojureOSGi.require(bundleContext, "clojure.tools.nrepl.server"); 
		//ClojureOSGi.require(bundleContext, "damp.ekeko"); 
		
	}

	public static void logError(String msg, Throwable e) {
		plugin.getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, msg, e));
	}

	public static void log (String msg) {
		plugin.getLog().log(new Status(IStatus.INFO, PLUGIN_ID, msg));
	}

	private ServerSocket ackREPLServer;

	public synchronized void startREPLServer() throws CoreException {
			if (ackREPLServer == null) {
		        try {
		        	Var startServer = BundleUtils.requireAndGetVar(getBundle().getSymbolicName(), "clojure.tools.nrepl.server/start-server");
		        	Object defaultHandler = BundleUtils.requireAndGetVar(
		        	        getBundle().getSymbolicName(),
		        	        "clojure.tools.nrepl.server/default-handler").invoke();
		        	Object handler = BundleUtils.requireAndGetVar(
		        	        getBundle().getSymbolicName(),
		        	        "clojure.tools.nrepl.ack/handle-ack").invoke(defaultHandler);
		            ackREPLServer = (ServerSocket)((Map)((Agent)startServer.invoke(Keyword.intern("handler"), handler)).deref()).get(Keyword.intern("ss"));
		            Activator.log("Started Ekeko-hosted nREPL server: nrepl://localhost:" + ackREPLServer.getLocalPort());
		        } catch (Exception e) {
		            Activator.logError("Could not start Ekeko-hosted nREPL server", e);
		            throw new CoreException(new Status(IStatus.ERROR, PLUGIN_ID,"Could not start plugin-hosted REPL server", e));
		        }
	    	}
			
			
			
		
	}
	
	

	public synchronized void myStartREPLServer() throws Exception {
		//starts repl in environment with correct class loader 
		ClojureOSGi.withBundle(getDefault().getBundle(), new RunnableWithException() {
			public Object run() throws Exception {
				startREPLServer();
				
				/*
				 * 
				 * java.lang.IllegalStateException: Can't change/establish root binding of: *ns* with set
	at clojure.lang.Var.set(Var.java:219)
	at clojure.lang.RT$1.invoke(RT.java:226)

				 * 
				 * 
				Object use = RT.readString("(use 'damp.ekeko)");
				clojure.lang.Compiler.eval(use);
				Object inns = RT.readString("(in-ns 'damp.ekeko)");
				clojure.lang.Compiler.eval(inns);
				
				*/
				
				return null;
			}});
		REPLView.connect(String.format("nrepl://%s:%s", "localhost", ackREPLServer.getLocalPort()));//CCWPlugin.getDefault().getREPLServerPort());
	}
	
	private void stopREPLServer() {
		if (ackREPLServer != null) {
			try {
				ackREPLServer.close();
			} catch (IOException e) {
				logError("Error while trying to close Ekeko-hosted nREPL server", e);
			}
		}
	}

	public int getREPLServerPort() throws Exception {
		if (ackREPLServer == null) {
			myStartREPLServer();
		}
		
		return ackREPLServer.getLocalPort();
	}

	public static Image getImage(String string) {
		if(pluginImages.get(string)==null){
			pluginImages.put(string, getImageDescriptor(string).createImage(true));
		}
		return pluginImages.get(string);
	}
	
	
	private void registerContributedFactories() throws CoreException{
		List<IProjectModelFactory> rps = new ArrayList<IProjectModelFactory>();
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IExtensionPoint p = registry.getExtensionPoint("ekeko.projectModelFactory");
		
		
		IExtension[] extensions = p.getExtensions();
		for (int j = 0; j < extensions.length; j++) {
			IExtension extension = extensions[j];
			IConfigurationElement[] confElems = extension
					.getConfigurationElements();
			for (int i = 0; i < confElems.length; i++) {
				IConfigurationElement iCE = confElems[i];
				Object ext = iCE.createExecutableExtension("factory");
				if (ext instanceof IProjectModelFactory) {
					IProjectModelFactory rp = (IProjectModelFactory) ext;
				//	if (rp.initialize(null, null, null)) {
						rps.add(rp);
				//	}
				}
			}

		}
		
		for(IProjectModelFactory factory : rps){
			EkekoModel.registerFactory(factory);
		}
	}
	


}
