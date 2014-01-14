package damp.ekeko;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator;

public class EkekoProblemFixer implements IMarkerResolutionGenerator {
	
	private static Map<String, Collection<IMarkerResolution>> fixes = new TreeMap<String, Collection<IMarkerResolution>>();
	
	
	
	
	@Override
	public IMarkerResolution[] getResolutions(IMarker marker) {
		try {
			if(marker.getType().equals(EkekoPlugin.EKEKO_PROBLEM_MARKER)){
				String kindOfMarker = marker.getAttribute("ekekoKind", "none");
				if(fixes.containsKey(kindOfMarker)){
					Collection<IMarkerResolution> res = fixes.get(kindOfMarker);
					return res.toArray(new IMarkerResolution[res.size()]);
				}
			}
		} catch (CoreException e) {
			e.printStackTrace();
			return new IMarkerResolution[]{};
		}
		return new IMarkerResolution[]{};
	}
	
	
	public static void installNewResolution(String kind, IMarkerResolution resolution){
		Collection<IMarkerResolution> coll;
		if(!fixes.containsKey(kind)){
			coll = new ArrayList<IMarkerResolution>();
			fixes.put(kind, coll);
		} else {
			coll = fixes.get(kind);
		}
		coll.add(resolution);
	}
	
	public static void resetFixes(){
		fixes = new TreeMap<String, Collection<IMarkerResolution>>();
	}

}
