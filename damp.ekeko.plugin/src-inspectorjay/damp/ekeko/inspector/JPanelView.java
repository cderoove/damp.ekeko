package damp.ekeko.inspector;

import java.awt.Frame;

import javax.swing.JApplet;
import javax.swing.JPanel;

import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

/**
 * A ViewPart that embeds a Swing JPanel
 * (Based on this article: http://www.eclipse.org/articles/article.php?file=Article-Swing-SWT-Integration/index.html)
 * @author Tim Molderez
 */
public class JPanelView extends ViewPart {

	public static final String ID = "damp.ekeko.inspector.JPanelView";
	private JApplet applet;
	
	public JPanelView() {}

	public void createPartControl(Composite parent) {
		// Create a JApplet (Swing component) within an SWT Composite
		Composite comp = new Composite(parent, SWT.EMBEDDED | SWT.NO_BACKGROUND);
		Frame frame = SWT_AWT.new_Frame(comp);
		applet = new JApplet();
	    frame.add(applet);
	}
	
	/**
	 * Set which JPanel should be shown in the view
	 * @param panel
	 */
	public void setJPanel(JPanel panel) {
		applet.setContentPane(panel);
	}

	public void setFocus() {}
}