package barista;

import java.util.Map;

public interface IResults {
	
	public int getSize();

	public Map toMap();
	
	public boolean isSuccess();
	
	public long getElapsedTime();
	
}
