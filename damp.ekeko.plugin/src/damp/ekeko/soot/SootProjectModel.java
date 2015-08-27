package damp.ekeko.soot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.LibraryLocation;
import org.eclipse.swt.widgets.Display;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Iterators;

import damp.ekeko.EkekoPlugin;
import damp.ekeko.EkekoProjectPropertyPage;
import damp.ekeko.ProjectModel;
import damp.ekeko.soot.icfg.Context;
import damp.ekeko.soot.icfg.Reachability;
import soot.Scene;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

public class SootProjectModel extends ProjectModel {
	
	private static final int MAX_CACHE_SIZE = 10000;
	private boolean stale = false;
	private Scene scene;
	private IJavaProject javaProject;
	
	private String classpath = ""; 
		
	private Cache<SootMethod, Iterable<SootMethod>> allDynamicMethodCalleesCache;
	
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
		allDynamicMethodCalleesCache = CacheBuilder.newBuilder().maximumSize(MAX_CACHE_SIZE).build();
	}

	public void populate(IProgressMonitor monitor) throws CoreException {
		super.populate(monitor);
		EkekoPlugin.getConsoleStream().println("Populating SootProjectModel for: " + getProject().getName());
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
		//computeReachability();
	}
	
	private void startSoot() {
		soot.G.reset();
		String[] args =  sootMainArguments();
		EkekoPlugin.getConsoleStream().println("Starting Soot with arguments: " + Arrays.toString(args));
		EkekoSootMain.main(args);
		EkekoPlugin.getConsoleStream().println("Completed Soot analyses.");

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
		result.add(classPathArgument());
		result.addAll(sootArgs());
		result.add("on");
		result.add("-main-class");
		result.add(entryPoint());
		result.add(entryPoint());
		
		return result.toArray(new String[result.size()]);
	}

	private String classPathArgument() {
		//ugly, but I'm getting JavaRuntime crashes otherwise
		Display.getDefault().syncExec(new Runnable() {
			public void run() {
				classpath = classPathForBaseProgram() + getClassPathSeparator() + classPathForJavaRTE();
			}
		});
		return classpath;
	}

	private String entryPoint() {
		try {
			return getProject().getPersistentProperty(EkekoProjectPropertyPage.ENTRYPOINT_PROPERTY);
		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}
	
	private List<String> sootArgs() {
		String args = null;
		try {
			args = getProject().getPersistentProperty(EkekoProjectPropertyPage.SOOTARGS_PROPERTY);
		} catch (CoreException e) {
			e.printStackTrace();
		}
		if(args == null)
			return Arrays.asList(DEFAULT_SOOTARGS.split(" "));
		else
			return Arrays.asList(args.split(" ")); 
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
	
	
	public Iterable<SootMethod> allDynamicMethodCallees(SootMethod m) {
		Set<SootMethod> callees = new HashSet<>();
		LinkedList<SootMethod> worklist = new LinkedList<>();
		worklist.add(m);
		while(!worklist.isEmpty()) {
			SootMethod method = worklist.removeFirst();
			Iterator<SootMethod> dynamicMethodCalleesIt = this.dynamicMethodCallees(method);
			while(dynamicMethodCalleesIt.hasNext()) {
				SootMethod callee = dynamicMethodCalleesIt.next();
				if(!callees.contains(callee)) {
					callees.add(callee);
					worklist.add(callee);
				}
			}
		}
		return callees;
	}
	
	public Iterable<SootMethod> allDynamicMethodCalleesCached(final SootMethod m) throws Exception {
		return allDynamicMethodCalleesCache.get(m, new Callable<Iterable<SootMethod>>() {
			@Override
			public Iterable<SootMethod> call() throws Exception {
				return allDynamicMethodCallees(m);
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

	
	
	
	//relied upon by Ekeko for AspectJ, better not use for other purposes
	
	/*
	public boolean preceeds(SootMethod pred, SootMethod succ) {
		JimpleBasedInterproceduralCFG icfg = new JimpleBasedInterproceduralCFG();		
		Set<Unit> visitedCallSites = new HashSet<>();
		LinkedList<Unit> worklist = new LinkedList<>();
		worklist.addAll(icfg.getStartPointsOf(succ));
		while(!worklist.isEmpty()) {
			Unit currentUnit = worklist.removeFirst();
			if(icfg.isCallStmt(currentUnit)) {
				visitedCallSites.add(currentUnit);
				SootMethod currentMethod = icfg.getMethodOf(currentUnit);
				if(pred.equals(currentMethod))
					return true;
			}
			
			List<Unit> predsOf = icfg.getPredsOf(currentUnit);
			predsOf.removeAll(visitedCallSites);
			worklist.addAll(predsOf);
		}
		return false;	
	}
	
	public boolean succeeds(SootMethod pred, SootMethod succ) {
		JimpleBasedInterproceduralCFG icfg = new JimpleBasedInterproceduralCFG();		
		Set<Unit> visitedCallSites = new HashSet<>();
		LinkedList<Unit> worklist = new LinkedList<>();
		worklist.addAll(icfg.getStartPointsOf(pred));
		while(!worklist.isEmpty()) {
			Unit currentUnit = worklist.removeFirst();
			if(icfg.isCallStmt(currentUnit)) {
				visitedCallSites.add(currentUnit);
				SootMethod currentMethod = icfg.getMethodOf(currentUnit);
				if(pred.equals(currentMethod))
					return true;
			}
			
			List<Unit> succsof = icfg.getSuccsOf(currentUnit);
			succsof.removeAll(visitedCallSites);
			worklist.addAll(succsof);
		}
		return false;	
	}
	*/
	
	/*
	
	private void computeReachability() {
		
		try { ProgramFlowGraph.createInstance(DUAAnalysis.getCFGFactory()); }
		catch (EntryNotFoundException e) { throw new RuntimeException(e.getMessage()); }
		
		List<SootMethod> entryMethods = ProgramFlowGraph.inst().getEntryMethods();
		ReachabilityAnalysis.computeReachability(entryMethods);
		EkekoPlugin.getConsoleStream().println("Completed reachability analysis");
	}
	
	
	public boolean onSamePath(Stmt pred, Stmt succ) {
		CFGNode predNode = ProgramFlowGraph.inst().getNode(pred);
		CFGNode succNode = ProgramFlowGraph.inst().getNode(succ);
		return ReachabilityAnalysis.reachesFromBottom(predNode, succNode, true);		
	}
	
	
	public boolean onSamePath(SootMethod pred, SootMethod succ) {
		CFG predCFG = ProgramFlowGraph.inst().getCFG(pred);
		CFG succCFG = ProgramFlowGraph.inst().getCFG(succ);
		if(predCFG == null || succCFG == null)
			return false;
		CFGNode predNode = predCFG.getFirstRealNode();
		CFGNode succNode = succCFG.getFirstRealNode();
		return ReachabilityAnalysis.reachesFromBottom(predNode, succNode, true);		
	}
	*/
	
	public boolean onSameExecutionPath(SootMethod pred, SootMethod succ) {
		Reachability reachability = new Reachability(this);
		Context callstack = reachability.onSameExecutionPath(pred, succ);
		return callstack != null;
	}
	
	
	

	
	
}
