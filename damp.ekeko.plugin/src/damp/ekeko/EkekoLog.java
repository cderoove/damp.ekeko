package damp.ekeko;

import java.io.PrintStream;

/**
 * Small logging facade used by the parts of the Ekeko model
 * (JavaProjectModel, EkekoModel, EkekoNature) that need to log progress
 * messages but shouldn't otherwise depend on EkekoPlugin -- which extends
 * org.eclipse.ui.plugin.AbstractUIPlugin and therefore requires an Eclipse
 * workbench (org.eclipse.ui) to even be class-loaded.
 *
 * Defaults to System.out so the model classes work in any host (including
 * a headless OSGi runtime with no workbench, e.g. a language server).
 * EkekoPlugin overrides this at plugin startup to route messages to the
 * "Ekeko Console" view instead, preserving the original Eclipse plugin's
 * behavior exactly.
 */
public class EkekoLog {

	private static volatile PrintStream stream = System.out;

	public static void setStream(PrintStream out) {
		stream = out != null ? out : System.out;
	}

	public static PrintStream getStream() {
		return stream;
	}

	public static void println(String msg) {
		stream.println(msg);
	}
}
