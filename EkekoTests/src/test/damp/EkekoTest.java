package test.damp;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.dialogs.IOverwriteQuery;
import org.eclipse.ui.wizards.datatransfer.FileSystemStructureProvider;
import org.eclipse.ui.wizards.datatransfer.ImportOperation;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import ccw.util.osgi.ClojureOSGi;
import ccw.util.osgi.RunnableWithException;
import clojure.lang.RT;
import clojure.lang.Symbol;
import clojure.lang.Var;
import damp.ekeko.EkekoPlugin;


//return URI.createURI(getClass().getResource("/resources/" + resourcePath).toExternalForm());

public class EkekoTest {
	
	static {
		
	}
	
	public static void ensureProjectImported(String projectName) throws Exception {
		//Bundle bundle = FrameworkUtil.getBundle(EkekoTest.class);
		//URL entry = bundle.getResource("/resources/TestCases/" + projectName + "/.project");
		//String fileName = entry.getFile();
		
		Bundle bundle = FrameworkUtil.getBundle(EkekoTest.class);
		URL fileURL = bundle.getResource("/resources/TestCases/" + projectName + "/.project");
		File file = new File(FileLocator.resolve(fileURL).toURI());
		
		//URL url = new URL("platform:/plugin/damp.ekeko.plugin.test/resources/TestCases/" + projectName + "/.project");
		//URLConnection connection = url.openConnection();
		//InputStream inputStream = connection.getInputStream();
		
		ensureProjectImported(file);
		
		//URL url = EkekoTest.class.getResource("./resources/TestCases/" + projectName + "/.project");
		//String path = url.getFile();
		
		
	}
		
	
	public static void ensureProjectImported(File dotProjectFile) throws Exception {
		InputStream inputStream = new FileInputStream(dotProjectFile);
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IProjectDescription loadedProjectDescription = workspace.loadProjectDescription(inputStream);
		String projectName = loadedProjectDescription.getName();
		IProject project = workspace.getRoot().getProject(projectName);
		if(project.exists())
			return;
		
		System.out.println("Importing project into namespace: " + projectName);
		
		IOverwriteQuery overwriteQuery = new IOverwriteQuery() {
			public String queryOverwrite(String file) { return NO_ALL; }
		};

		
		final ImportOperation importOperation = new ImportOperation(project.getFullPath(), dotProjectFile, FileSystemStructureProvider.INSTANCE, overwriteQuery);
		importOperation.setCreateContainerStructure(true); //copies into ws such that original is not modified
		Display.getDefault().asyncExec(new Runnable() {			
			@Override
			public void run() {
				try {
					importOperation.run(new NullProgressMonitor());
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
			}
		});	
		
		System.out.println("Project: " + project);

		
	}
	
	
	@BeforeClass
	public static void ensureTestCasesExist() throws Exception {
		ensureProjectImported("Ekeko-JDT");
	}

	@Test
	public void testPluginID() {
		assertEquals(EkekoPlugin.PLUGIN_ID, "damp.ekeko.plugin");		
	}
	
	@Test 
	public void testEkekoSuite() {
		testClojureNamespace("test.damp.ekeko");
	}
	

	@Test 
	public void testEkekoJDTSuite() {
		testClojureNamespace("test.damp.ekeko.jdt");
	}
	
	public static void testClojureNamespace(final String namespace) {
		Bundle ekekoBundle = FrameworkUtil.getBundle(EkekoTest.class);
		
		ClojureOSGi.require(ekekoBundle, "clojure.test");
		ClojureOSGi.require(ekekoBundle, namespace);
		
		final Var successful = RT.var("clojure.test", "successful?");
		 
		ClojureOSGi.withBundle(ekekoBundle, new RunnableWithException() {
			@Override
			public Object run() throws Exception {
				Object testResults = RT.var("clojure.test", "run-tests").invoke(Symbol.intern(namespace));
				System.out.println(testResults);
				Boolean success = (Boolean) successful.invoke(testResults);
				
				if(!success) 
					throw new Exception(testResults.toString());
				return null;
			}
		});
	
	}
	
	
	


}
