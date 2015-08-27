package damp.ekeko.soot.icfg;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import damp.ekeko.soot.SootProjectModel;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;

public class Reachability {
	
	private JimpleBasedInterproceduralCFG icfg;
	private SootProjectModel sootProjectModel;
	
	public Reachability(SootProjectModel sootProjectModel) {
		this.icfg = new JimpleBasedInterproceduralCFG();
		this.sootProjectModel = sootProjectModel;
	}
	
	
	public Context onSameExecutionPath(SootMethod source, SootMethod target) {
		List<SootMethod> entryPoints = sootProjectModel.getScene().getEntryPoints();
		for(SootMethod entryPoint : entryPoints) {
			Context reachedContext = inDynamicExtent(entryPoint, target);
			if(reachedContext != null)
				return reachedContext;
		}
		return null;

	}
	
	public Context inDynamicExtent(SootMethod source, SootMethod target) {
		for(Unit start : icfg.getStartPointsOf(source)) {
			Context startingContext = new Context(start);
			Context reached = reachable(startingContext, target);
			if(reached != null)
				return reached;
		}
		return null;
	}


	
	public Context reachable(Context source, SootMethod target) {
		
		LinkedList<Context> worklist = new LinkedList<Context>();
		HashSet<Context> marked = new HashSet<>();
		worklist.add(source);
		marked.add(source);
		
		while(!worklist.isEmpty()) {
			Context current = worklist.poll();
			
			Unit reachedNode = current.getReachedNode();
			
			SootMethod reachedMethod = icfg.getMethodOf(reachedNode);
			if(reachedMethod.equals(target))
				return current;
			
			//call
			if(icfg.isCallStmt(reachedNode)) {
				Collection<SootMethod> callees = icfg.getCalleesOfCallAt(reachedNode);
				for(SootMethod callee : callees) {
					for (Unit unit : icfg.getStartPointsOf(callee)) {
						Context down =  current.descend(unit, reachedNode);	
						if(marked.add(down))
							worklist.add(down);
					}
				}
				continue;
			}
			
			//return
			if(icfg.isExitStmt(reachedNode)) {
				Unit callSite = current.top();
				List<Unit> succs = icfg.getSuccsOf(callSite);
				for(Unit succ : succs) {
					Context up = current.ascend(succ);
					if(marked.add(up))
						worklist.add(up);
				}
				continue;
			}
			
			//intra-procedural
			List<Unit> succs = icfg.getSuccsOf(reachedNode);
			for(Unit succ : succs) {
				Context up = current.level(succ);
				if(marked.add(up))
					worklist.add(up);
			}	
		}
		
		return null;
	}
	
	
	
}
