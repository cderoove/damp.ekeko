package damp.ekeko.jdtls;

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import clojure.lang.RT;

/**
 * Starts/stops an nREPL server inside this bundle's own JVM/classloader, so
 * a connected client (e.g. Calva) can evaluate Clojure code with full,
 * direct access to the live Ekeko query engine and JDT model running here.
 * The actual work (and the server state) lives in damp/ekeko/nrepl.clj;
 * this class only bridges to it from Java.
 *
 * damp.ekeko.plugin's own REPLController did the equivalent via
 * Counterclockwise's ClojureOSGi.withBundle/BundleUtils, which aren't
 * available here (this bundle doesn't depend on ccw). The standard
 * replacement for embedding Clojure in an arbitrary OSGi bundle is to set
 * the bundle's own classloader as the calling thread's context classloader
 * for the duration of the Clojure API calls (Clojure's dynamic namespace
 * loading resolves .clj resources via the context classloader), then
 * restore it afterwards.
 */
public final class EkekoReplController {

	private static final String NREPL_NS = "damp.ekeko.nrepl";

	private EkekoReplController() {
	}

	public static int startRepl() throws Exception {
		return startRepl(null);
	}

	/**
	 * @param timeoutMs milliseconds after which a still running "eval"
	 *                  request is interrupted; {@code null} or non-positive
	 *                  for no timeout. Ignored when a server is already
	 *                  running.
	 */
	public static int startRepl(Long timeoutMs) throws Exception {
		return ((Number) invoke("start-repl", timeoutMs)).intValue();
	}

	public static void stopRepl() throws Exception {
		invoke("stop-repl");
	}

	public static boolean isRunning() {
		return (Boolean) invoke("running?");
	}

	public static int getPort() {
		return ((Number) invoke("port")).intValue();
	}

	private static Object invoke(String fnName, Object... args) {
		ClassLoader previous = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(EkekoReplController.class.getClassLoader());
		try {
			IFn require = Clojure.var("clojure.core", "require");
			require.invoke(Clojure.read(NREPL_NS));
			return Clojure.var(NREPL_NS, fnName).applyTo(RT.seq(args));
		} finally {
			Thread.currentThread().setContextClassLoader(previous);
		}
	}
}
