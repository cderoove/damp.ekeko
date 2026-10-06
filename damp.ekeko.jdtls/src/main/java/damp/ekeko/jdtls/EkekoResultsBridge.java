package damp.ekeko.jdtls;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin;

/**
 * Called from this bundle's own {@code damp.ekeko.gui} namespace (see
 * {@code src/main/resources/damp/ekeko/gui.clj}) -- the jdt.ls-side stand-in
 * for the Eclipse plugin's SWT-based {@code damp.ekeko.gui}, resolved the
 * same way via {@code requiring-resolve} from {@code ekeko*}/{@code
 * ekeko-n*} in {@code damp/ekeko.clj}, unchanged there. Instead of opening a
 * Barista SWT view, pushes the finished query's results to the VS Code
 * extension host as a {@code workspace/notify} notification (fire-and-
 * forget, per {@code JavaClientConnection.sendNotification}/{@code
 * ExecuteCommandProposedClient}), where {@code ekeko.showResults} opens/
 * updates the results webview -- reachable from any client-registered VS
 * Code command, not just ones jdt.ls itself advertises, since the LSP
 * client library dispatches {@code workspace/notify} via a plain {@code
 * vscode.commands.executeCommand}.
 *
 * {@link #queryStarted}/{@link #queryFinished} likewise notify the extension
 * host around each query, so it can show that one is running.
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

	public static void showResults(String queryText, Object varsSeq, Object results, Object elapsedMs, Object count) {
		List<String> vars = EkekoQueryRunner.symbolNames(varsSeq);
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("query", queryText);
		payload.put("vars", vars);
		payload.put("rows", EkekoQueryRunner.serializeRows(results));
		payload.put("elapsedMs", elapsedMs);
		payload.put("count", count);
		notifyClient("ekeko.showResults", payload);
	}

	private static void notifyClient(String command, Object... arguments) {
		NOTIFIER.execute(() -> JavaLanguageServerPlugin.getInstance().getClientConnection()
				.sendNotification(command, arguments));
	}
}
