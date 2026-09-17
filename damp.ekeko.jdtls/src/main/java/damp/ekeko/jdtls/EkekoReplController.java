package damp.ekeko.jdtls;

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import clojure.lang.Keyword;

import java.util.Map;

/**
 * Starts/stops an nREPL server inside this bundle's own JVM/classloader, so
 * a connected client (e.g. Calva) can evaluate Clojure code with full,
 * direct access to the live Ekeko query engine and JDT model running here.
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

	private static Object server;
	private static int port = -1;

	private EkekoReplController() {
	}

	public static synchronized int startRepl() throws Exception {
		if (server != null) {
			return port;
		}
		ClassLoader previous = Thread.currentThread().getContextClassLoader();
		ClassLoader bundleLoader = EkekoReplController.class.getClassLoader();
		Thread.currentThread().setContextClassLoader(bundleLoader);
		try {
			IFn require = Clojure.var("clojure.core", "require");
			require.invoke(Clojure.read("nrepl.server"));
			IFn defaultHandler = Clojure.var("nrepl.server", "default-handler");
			IFn startServer = Clojure.var("nrepl.server", "start-server");
			Object handler = defaultHandler.invoke();
			Object startedServer = startServer.invoke(Keyword.intern("handler"), handler);
			@SuppressWarnings("unchecked")
			Map<Object, Object> serverMap = (Map<Object, Object>) startedServer;
			port = (Integer) serverMap.get(Keyword.intern("port"));
			server = startedServer;
			return port;
		} finally {
			Thread.currentThread().setContextClassLoader(previous);
		}
	}

	public static synchronized void stopRepl() throws Exception {
		if (server == null) {
			return;
		}
		ClassLoader previous = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(EkekoReplController.class.getClassLoader());
		try {
			IFn stopServer = Clojure.var("nrepl.server", "stop-server");
			stopServer.invoke(server);
		} finally {
			Thread.currentThread().setContextClassLoader(previous);
			server = null;
			port = -1;
		}
	}

	public static synchronized boolean isRunning() {
		return server != null;
	}

	public static synchronized int getPort() {
		return port;
	}
}
