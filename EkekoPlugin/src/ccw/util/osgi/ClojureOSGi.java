package ccw.util.osgi;

import org.osgi.framework.Bundle;

import clojure.lang.Compiler;
import clojure.lang.IPersistentMap;
import clojure.lang.RT;
import clojure.lang.Symbol;
import clojure.lang.Var;
import damp.ekeko.Activator;

public class ClojureOSGi {
	private static volatile boolean initialized;
	private synchronized static void initialize() {
		if (initialized) return;
		
		System.out.println("ClojureOSGi: Static initialization, loading clojure.core");
		System.out.flush();
		ClassLoader loader = new BundleClassLoader(Activator.getDefault().getBundle());
		ClassLoader saved = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(loader);
			Class.forName("clojure.lang.RT", true, loader); // very important, uses the right classloader
		} catch (Exception e) {
			throw new RuntimeException(
					"ClojureOSGi: Static initialization, Exception while loading clojure.core", e);
		} finally {
			Thread.currentThread().setContextClassLoader(saved);
		}
		System.out.println("ClojureOSGi: Static initialization, clojure.core loaded");
		System.out.flush();
		
		initialized = true;
	}
	public synchronized static Object withBundle(Bundle aBundle, RunnableWithException aCode)
			throws RuntimeException {
		
		initialize();
		
		ClassLoader loader = new BundleClassLoader(aBundle);
		IPersistentMap bindings = RT.map(Compiler.LOADER, loader);

		boolean pushed = true;

		ClassLoader saved = Thread.currentThread().getContextClassLoader();

		try {
			Thread.currentThread().setContextClassLoader(loader);

			try {
				Var.pushThreadBindings(bindings);
			} catch (RuntimeException aEx) {
				pushed = false;
				throw aEx;
			}

			return aCode.run();
			
		} catch (Exception e) {
			throw new RuntimeException(
					"Exception while calling withBundle(" 
							+ aBundle.getSymbolicName() + ", aCode)",
					e);
		} finally {
			if (pushed)
				Var.popThreadBindings();

			Thread.currentThread().setContextClassLoader(saved);
		}
	}

	public synchronized static void require(final Bundle bundle, final String namespace) {
		System.out.println("ClojureOSGi.require(" + bundle.getSymbolicName() + ", " 
				+ namespace + ")");
		ClojureOSGi.withBundle(bundle, new RunnableWithException() {
			@Override
			public Object run() throws Exception {
				try {
					RT.var("clojure.core", "require").invoke(Symbol.intern(namespace));
					return null;
				} catch (Exception e) {
					throw e;
				}
			}
		});
	}
}
