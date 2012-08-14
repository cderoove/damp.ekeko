package damp.ekeko;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Iterator;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
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

public class WholeProgramAnalysisJavaProjectModel extends JavaProjectModel {
	
	private boolean stale = false;
	private Scene scene;

	public WholeProgramAnalysisJavaProjectModel(IProject p) {
		super(p);
	}

	public void clean() {
		super.clean();
		stale = false;
	}

	public void populate(IProgressMonitor monitor) throws CoreException {
		super.populate(monitor);
		System.out.println("Populating WholeProgramAnalysisModel for: " + getProject().getName());
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

		String geometricString = "-soot-classpath " + classPathForBaseProgram() + ":" + classPathForJavaRTE() +
				" -src-prec c -f jimple -keep-line-number -app -w -p jb use-original-names:true " +
				"-p cg.spark geom-pta:true,geom-trans:false -p jap.npc on -main-class "//,geom-eval:2 for some stats
				+ entryPoint()
				+ " " 
				+ entryPoint();
		
		String refinementString = "-soot-classpath " + classPathForBaseProgram() + ":" + classPathForJavaRTE() +
				" -src-prec c -f jimple -keep-line-number -app -w -p jb use-original-names:true " +
				"-p cg.spark cs-demand:true -p jap.npc on -main-class "// lazy-pts:true
				+ entryPoint()
				+ " " 
				+ entryPoint();
		
		return refinementString.split(" ");
	}

	private String entryPoint() {
		try {
			return getProject().getPersistentProperty(EkekoProjectPropertyPage.ENTRYPOINT_PROPERTY);
		} catch (CoreException e) {
			e.printStackTrace();
			return "";
		}
	}

	private String classPathForJavaRTE() {
		try {
			IVMInstall vmInstall = JavaRuntime.getVMInstall(getJavaProject());
			LibraryLocation[] locs = JavaRuntime.getLibraryLocations(vmInstall);
			String[] stringlocs = new String[locs.length];
			int i=0;
			for(LibraryLocation loc : locs)
				stringlocs[i++]=loc.getSystemLibraryPath().toOSString();
			return Joiner.on(":").join(stringlocs);
		} catch (CoreException e) {
			e.printStackTrace();
			return "";
		}
	}

	private String classPathForBaseProgram() {
		try {
			return Joiner.on(":").join(JavaRuntime.computeDefaultRuntimeClassPath(getJavaProject()));
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
