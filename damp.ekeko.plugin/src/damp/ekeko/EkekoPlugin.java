package damp.ekeko;

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
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import ccw.util.osgi.ClojureOSGi;
//import ccw.repl.REPLView;

public class EkekoPlugin extends AbstractUIPlugin {

	public static final String PLUGIN_ID = "damp.ekeko.plugin"; //$NON-NLS-1$
	public static final String EKEKO_PROBLEM_MARKER = "damp.ekeko.plugin.ekekoproblemmarker";

	private static EkekoPlugin plugin;

	private static Map<String, Image> pluginImages = new HashMap<String, Image>();

	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		EkekoModel.registerDefaultFactories();
		this.registerContributedFactories();
		startClojureCode(context);		
		EkekoModel.getInstance().populate();
	
	}

	public void stop(BundleContext context) throws Exception {
		REPLController.getCurrent().stopREPLServer();
		plugin = null;
		super.stop(context);
	}
	
	public static EkekoPlugin getDefault() {
		return plugin;
	}

	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}

	private void startClojureCode(BundleContext bundleContext) throws Exception {
		Bundle b = bundleContext.getBundle();
		ClojureOSGi.require(b, "clojure.stacktrace"); 
		ClojureOSGi.require(b, "clojure.test");  
		ClojureOSGi.require(b, "clojure.tools.nrepl.server"); 
		ClojureOSGi.require(b, "ccw.debug.serverrepl"); 	
	}

	public static void logError(String msg, Throwable e) {
		plugin.getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, msg, e));
	}

	public static void log (String msg) {
		plugin.getLog().log(new Status(IStatus.INFO, PLUGIN_ID, msg));
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
	
	private static String CONSOLE_NAME = "Ekeko Console";
	
	public static MessageConsole getConsole() {
	      ConsolePlugin plugin = ConsolePlugin.getDefault();
	      IConsoleManager conMan = plugin.getConsoleManager();
	      
	      
	      
	      for(IConsole existing : conMan.getConsoles()) {
	    	  if(CONSOLE_NAME.equals(existing.getName()))
	    		  return (MessageConsole) existing;
	      }

	      MessageConsole myConsole = new MessageConsole(CONSOLE_NAME, getImageDescriptor("icons/ekeko16.png"));
	      conMan.addConsoles(new IConsole[]{myConsole});
	      myConsole.activate();
	      return myConsole;	      
	}
	
	
	private static MessageConsoleStream consoleStream;
	
	public static MessageConsoleStream getConsoleStream() {
		if(consoleStream == null || consoleStream.isClosed()) {
			consoleStream = getConsole().newMessageStream();
		}
		return consoleStream;
		
	}
	

}
