package test.damp;


import java.io.File;
import java.net.URL;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.osgi.framework.Bundle;

import ccw.util.osgi.ClojureOSGi;
import ccw.util.osgi.RunnableWithException;
import clojure.lang.RT;
import clojure.lang.Symbol;
import clojure.lang.Var;


//return URI.createURI(getClass().getResource("/resources/" + resourcePath).toExternalForm());

public class EkekoTestHelper {
		
	public static void ensureProjectImported(Bundle bundle, String enclosingFolderRelativeToBundle, String projectName) throws Exception  {
		//Bundle bundle = FrameworkUtil.getBundle(EkekoTest.class);
		//URL entry = bundle.getResource("/resources/TestCases/" + projectName + "/.project");
		//String fileName = entry.getFile();
		
		//Bundle bundle = FrameworkUtil.getBundle(EkekoTestHelper.class);
		URL fileURL = bundle.getResource(enclosingFolderRelativeToBundle + projectName + "/.project");
		File file;
		try {
			file = new File(FileLocator.resolve(fileURL).toURI());
		} catch (Exception e) {
			System.out.println("Could not resolve project URL:" + enclosingFolderRelativeToBundle + projectName + "/.project");
			e.printStackTrace();
			throw e;
		}
		
		//URL url = new URL("platform:/plugin/damp.ekeko.plugin.test/resources/TestCases/" + projectName + "/.project");
		//URLConnection connection = url.openConnection();
		//InputStream inputStream = connection.getInputStream();
		
		ensureProjectImported(file);
		
		//URL url = EkekoTest.class.getResource("./resources/TestCases/" + projectName + "/.project");
		//String path = url.getFile();
		
		
	}
		

	public static void ensureProjectImported(File dotProjectFile) throws CoreException, OperationCanceledException, InterruptedException {

		IProjectDescription description = ResourcesPlugin.getWorkspace().loadProjectDescription(new Path(dotProjectFile.getAbsolutePath()));
		System.out.println("Importing project into workspace: " + description.getName());
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(description.getName());
		project.create(description, null);
		project.open(null);
		System.out.println("Waiting for project to build: " + project);
		waitForWorkspaceBuildsToFinish();
	}
	
	/*
	public static void ensureProjectImported(File dotProjectFile) throws Exception {
		InputStream inputStream = new FileInputStream(dotProjectFile);
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IProjectDescription loadedProjectDescription = workspace.loadProjectDescription(inputStream);
		String projectName = loadedProjectDescription.getName();
		final IProject project = workspace.getRoot().getProject(projectName);
		if(project.exists())
			return;
		
		System.out.println("Importing project into namespace: " + projectName);
		
		IOverwriteQuery overwriteQuery = new IOverwriteQuery() {
			public String queryOverwrite(String file) { return ALL; }
		};

		
		final ImportOperation importOperation = new ImportOperation(project.getFullPath(), dotProjectFile, FileSystemStructureProvider.INSTANCE, overwriteQuery);
		importOperation.setCreateContainerStructure(true); 
		Display.getDefault().asyncExec(new Runnable() {			
			@Override
			public void run() {
				try {
					importOperation.run(new NullProgressMonitor());
					//project.open(null);
					
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				} 
				
			}
		});	
		
		
		System.out.println("Waiting for project to build: " + project);
		waitForWorkspaceBuildsToFinish();

		

		
	}

	*/
	
	public static void waitForWorkspaceBuildsToFinish() throws OperationCanceledException, InterruptedException {
		IJobManager jobManager = Job.getJobManager();
		jobManager.join(ResourcesPlugin.FAMILY_AUTO_BUILD, null);
		System.out.println("Build finished.");
	}	
	
	
	
	public static void testClojureNamespace(Bundle bundle, final String namespace) {
		
		
		//Bundle ekekoBundle = FrameworkUtil.getBundle(EkekoTestHelper.class);
		
		ClojureOSGi.require(bundle, "clojure.test");
		ClojureOSGi.require(bundle, namespace);
		
		final Var successful = RT.var("clojure.test", "successful?");
		 
		ClojureOSGi.withBundle(bundle, new RunnableWithException() {
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
