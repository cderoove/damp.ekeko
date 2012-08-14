package baristaui.views.queryResult.table;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

import barista.IResults;

public class IResultContentProvider implements IStructuredContentProvider {

	Map<String, List<Object>> results;
	private String[] variables;

	public IResultContentProvider(Map<String, List<Object>> results2, String[] variables) {
		this.results = results2;
		this.variables = variables;
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub

	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		// TODO Auto-generated method stub

	}

	@SuppressWarnings("unchecked") //Ugh
	@Override
	public Object[] getElements(Object inputElement) {
		if (inputElement instanceof Map) {
			Map<String,List<Object>> results = (Map<String,List<Object>>) inputElement;

			Collection<Map<String,Object>> accumulator = new ArrayList<Map<String,Object>>();
			if(variables.length == 0) return accumulator.toArray(); //no results
			int resultSize = this.results.get(variables[0]).size();
			
			for(int i = 0; i<resultSize;i++){
				HashMap<String, Object> map = new HashMap<String, Object>();
				for(int j=0; j<variables.length;j++){
					map.put(variables[j], results.get(variables[j]).get(i));
				}
				accumulator.add(map);
			}
						
			
			return accumulator.toArray();
		}
		return null;
	}

}
