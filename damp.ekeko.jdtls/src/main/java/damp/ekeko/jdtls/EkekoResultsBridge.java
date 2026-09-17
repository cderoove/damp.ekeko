package damp.ekeko.jdtls;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
 */
public final class EkekoResultsBridge {

	private EkekoResultsBridge() {
	}

	public static void showResults(String queryText, Object varsSeq, Object results, Object elapsedMs, Object count) {
		List<String> vars = EkekoQueryRunner.symbolNames(varsSeq);
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("query", queryText);
		payload.put("vars", vars);
		payload.put("rows", EkekoQueryRunner.serializeRows(results));
		payload.put("elapsedMs", elapsedMs);
		payload.put("count", count);
		JavaLanguageServerPlugin.getInstance().getClientConnection().sendNotification("ekeko.showResults", payload);
	}
}
