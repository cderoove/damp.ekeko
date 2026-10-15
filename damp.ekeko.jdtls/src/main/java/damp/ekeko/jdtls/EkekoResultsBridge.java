package damp.ekeko.jdtls;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin;

import clojure.java.api.Clojure;

/**
 * Bridge to show the query results from ekeko. 
 *
 * Called from within Ekeko itself (see {@code src/main/resources/damp/ekeko/gui.clj}).
 */
public final class EkekoResultsBridge {

	/**
	 * Sends the notifications, in order. They must not be written to the
	 * client connection from the thread evaluating the query: when nREPL
	 * interrupts that thread (e.g. on an eval timeout), its interrupt flag
	 * stays set, and on macOS/Linux jdt.ls talks to the client over a socket
	 * channel, which a write from an interrupted thread fails and closes.
	 */
	private static final ExecutorService NOTIFIER = Executors.newSingleThreadExecutor(runnable -> {
		Thread thread = new Thread(runnable, "ekeko-client-notifier");
		thread.setDaemon(true);
		return thread;
	});

	private EkekoResultsBridge() {
	}

	public static void queryStarted(String id, String queryText) {
		notifyClient("ekeko.queryStarted", id, queryText);
	}

	/**
	 * @param expiredTimeoutMs the eval timeout that stopped the query, or
	 *                         {@code null} when it ended by itself
	 */
	public static void queryFinished(String id, Long expiredTimeoutMs) {
		// The client connection rejects null arguments.
		if (expiredTimeoutMs == null) {
			notifyClient("ekeko.queryFinished", id);
		} else {
			notifyClient("ekeko.queryFinished", id, expiredTimeoutMs);
		}
	}

	/**
	 * Interrupts the nREPL evaluation running the query with the given id,
	 * as given to {@link #queryStarted}. 
	 * @return whether that query was still running and has been cancelled
	 */
	public static boolean cancelQuery(String id) {
		ClassLoader previous = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(EkekoResultsBridge.class.getClassLoader());
		try {
			Clojure.var("clojure.core", "require").invoke(Clojure.read("damp.ekeko.gui"));
			return Boolean.TRUE.equals(Clojure.var("damp.ekeko.gui", "cancel-query").invoke(id));
		} finally {
			Thread.currentThread().setContextClassLoader(previous);
		}
	}

	public static void showResults(String queryText, Object varsSeq, Object results, Object elapsedMs, Object count) {
		List<String> vars = EkekoQueryRunner.symbolNames(varsSeq);
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("query", queryText);
		payload.put("vars", vars);
		List<List<Map<String, Object>>> rows = EkekoQueryRunner.serializeRows(results);
		payload.put("rows", rows);
		payload.put("classDocs", EkekoClassDocumentation.classDocs(rows));
		payload.put("elapsedMs", elapsedMs);
		payload.put("count", count);
		notifyClient("ekeko.showResults", payload);
	}

	private static void notifyClient(String command, Object... arguments) {
		NOTIFIER.execute(() -> JavaLanguageServerPlugin.getInstance().getClientConnection()
				.sendNotification(command, arguments));
	}
}
