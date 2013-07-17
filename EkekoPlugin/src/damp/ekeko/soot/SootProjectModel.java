package damp.ekeko.soot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.LibraryLocation;

import soot.Scene;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterators;

import damp.ekeko.EkekoProjectPropertyPage;
import damp.ekeko.ProjectModel;

public class SootProjectModel extends ProjectModel {
	
	private boolean stale = false;
	private Scene scene;
	private IJavaProject javaProject;
	
	public static final String DEFAULT_SOOTARGS = "-src-prec c -f jimple -keep-line-number -app -w -p jb use-original-names:true " +
			"-p cg.spark cs-demand:true -p jap.npc";

/*  Use these arguments to greatly speed up the Soot analysis. (The sacrifice is that the simple "class hierarchy analysis" is used, 
 * and *only* the application classes will be analysed (i.e. the standard Java libraries are excluded). */
//	public static final String DEFAULT_SOOTARGS = "-no-bodies-for-excluded -src-prec c -f jimple -keep-line-number -app -w -p cg.cha";

	public IJavaProject getJavaProject() {
		return javaProject;
	}

	public SootProjectModel(IProject p) {
		super(p);
		javaProject = JavaCore.create(p);
		clean();
	}

	public void clean() {
		super.clean();
		stale = false;
	}

	public void populate(IProgressMonitor monitor) throws CoreException {
		super.populate(monitor);
		System.out.println("Populating SootProjectModel for: " + getProject().getName());
		populateAnalysisInformation(monitor);
	}

	public void processDelta(IResourceDelta delta, IProgressMonitor monitor) throws CoreException {
		super.processDelta(delta, monitor);
		stale = true;
	}

	public boolean isStale() {
		return stale;
	}
	
	public Scene getScene() {
		return scene;
	}
	
	private void populateAnalysisInformation(IProgressMonitor monitor) {
		startSoot();
		scene = Scene.v();
		stale = false;
	}
	
	private void startSoot() {
		soot.G.reset();
		String[] args =  sootMainArguments();
		System.out.println("Starting Soot with arguments: " + Arrays.toString(args));
		EkekoSootMain.main(args);
	}
	
	private String[] sootMainArguments() {
		//mind the separators (i.e., a single space), see split statement

//		String geometricString = "-soot-classpath " + classPathForBaseProgram() + getClassPathSeparator() + classPathForJavaRTE() +
//				" -src-prec c -f jimple -keep-line-number -app -w -p jb use-original-names:true " +
//				"-p cg.spark geom-pta:true,geom-trans:false -p jap.npc on -main-class "//,geom-eval:2 for some stats
//				+ entryPoint()
//				+ " " 
//				+ entryPoint();
		
//		String refinementString = "-soot-classpath " + classPathForBaseProgram() + getClassPathSeparator() + classPathForJavaRTE() +
//				" -src-prec c -f jimple -keep-line-number -app -w -p jb use-original-names:true " +
//				"-p cg.spark cs-demand:true -p jap.npc on -main-class "// lazy-pts:true
//				+ entryPoint()
//				+ " " 
//				+ entryPoint();
		
		ArrayList<String> result = new ArrayList<String>();
		result.add("-soot-classpath");
		result.add(classPathForBaseProgram() + getClassPathSeparator() + classPathForJavaRTE());
		result.addAll(sootArgs());
		result.add("on");
		result.add("-main-class");
		result.add(entryPoint());
		result.add(entryPoint());
		
		return result.toArray(new String[result.size()]);
	}

	private String entryPoint() {
		try {
			return getProject().getPersistentProperty(EkekoProjectPropertyPage.ENTRYPOINT_PROPERTY);
		} catch (CoreException e) {
			e.printStackTrace();
			return "";
		}
	}
	
	private List<String> sootArgs() {
		try {	
			String args = getProject().getPersistentProperty(EkekoProjectPropertyPage.SOOTARGS_PROPERTY);
			return Arrays.asList(args.split(" ")); 
		} catch (CoreException e) {
			e.printStackTrace();
			return Arrays.asList(DEFAULT_SOOTARGS.split(" "));
		}
	}
	
	private String getClassPathSeparator() {
		return System.getProperty("path.separator");
	}
	
 	private String classPathForJavaRTE() {
		try {
			IVMInstall vmInstall = JavaRuntime.getVMInstall(getJavaProject());
			LibraryLocation[] locs = JavaRuntime.getLibraryLocations(vmInstall);
			String[] stringlocs = new String[locs.length];
			int i=0;
			
			for(LibraryLocation loc : locs)
				stringlocs[i++]=loc.getSystemLibraryPath().toOSString();
			
			return Joiner.on(getClassPathSeparator()).join(stringlocs);
		} catch (CoreException e) {
			e.printStackTrace();
			return "";
		}
	}

	private String classPathForBaseProgram() {
		try {
			return Joiner.on(getClassPathSeparator()).join(JavaRuntime.computeDefaultRuntimeClassPath(getJavaProject()));
		} catch(CoreException e) {
			e.printStackTrace();
			return "";
		}
	}

	//TODO: there is some incompatibility between LVAR and EDGE (classCastException in equals of the latter, they should do an instanceof check, report bug) 
	//... therefore temporarily implement the following methods on the java side
	public Iterator<SootMethod> dynamicMethodCallees(SootMethod m) {
		CallGraph graph = getScene().getCallGraph();
		Iterator<Edge> edgesOutOf = graph.edgesOutOf(m);
		return Iterators.transform(edgesOutOf, new Function<Edge,SootMethod>()  {
			public SootMethod apply(Edge e) {
				return e.tgt();
			}
		});
	}
	
	public Iterator<SootMethod> dynamicUnitCallees(Unit u) {
		CallGraph graph = getScene().getCallGraph();
		Iterator<Edge> edgesOutOf = graph.edgesOutOf(u);
		return Iterators.transform(edgesOutOf, new Function<Edge,SootMethod>()  {
			public SootMethod apply(Edge e) {
				return e.tgt();
			}
		});
	}
	
	public Iterator<SootMethod> dynamicMethodCallers(SootMethod m) {
		CallGraph graph = getScene().getCallGraph();
		Iterator<Edge> edgesInTo = graph.edgesInto(m);
		return Iterators.transform(edgesInTo, new Function<Edge,SootMethod>()  {
			public SootMethod apply(Edge e) {
				return e.src();
			}
		});
	}
	
	public Iterator<Unit> dynamicUnitCallers(SootMethod m) {
		CallGraph graph = getScene().getCallGraph();
		Iterator<Edge> edgesInto = graph.edgesInto(m);
		return Iterators.transform(edgesInto, new Function<Edge,Unit>()  {
			public Unit apply(Edge e) {
				return e.srcUnit();
			}
		});
	}

	
	
	

	
	
}
