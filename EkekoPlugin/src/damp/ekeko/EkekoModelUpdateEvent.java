package damp.ekeko;

public class EkekoModelUpdateEvent implements IEkekoModelUpdateEvent {
	
	public EkekoModelUpdateEvent(IProjectModel m) {
		model = m;
	}
	
	private IProjectModel model;
	
	public IProjectModel getModel() {
		return model;
	}
	
}
