package damp.ekeko.jdtls;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import clojure.lang.IPersistentVector;
import clojure.lang.Namespace;
import clojure.lang.RT;
import clojure.lang.Symbol;
import clojure.lang.Var;

/**
 * Runs an Ekeko query (typed by the user as plain Clojure source, e.g.
 * {@code (ekeko [?m] (ast :MethodDeclaration ?m))}) against the live
 * project model and serializes the result into a plain
 * {@code Map}/{@code List}/primitive shape that round-trips cleanly through
 * Gson (the {@code IDelegateCommandHandler} response is JSON-RPC-encoded by
 * jdt.ls). Mirrors {@code EkekoReplController}'s classloader-swap pattern
 * for calling into Clojure from this bundle, and
 * {@code baristaui.util.MarkerUtility}'s ASTNode -> source-location logic
 * for the jump-to-source case, minus the Eclipse-workbench/IMarker parts
 * that don't apply outside the Eclipse plugin.
 */
public final class EkekoQueryRunner {

	private EkekoQueryRunner() {
	}

	public static Map<String, Object> runQuery(String queryText) throws Exception {
		ClassLoader previous = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(EkekoQueryRunner.class.getClassLoader());
		try {
			IFn require = Clojure.var("clojure.core", "require");
			require.invoke(Clojure.read("damp.ekeko"));

			IFn readString = Clojure.var("clojure.core", "read-string");
			Object form = readString.invoke(queryText);

			// Evaluate with *ns* bound (not just in-ns'd) to damp.ekeko so the
			// query text's unqualified symbols (ast, has, child, ekeko, ...)
			// resolve the same way they do inside damp/ekeko.clj itself,
			// without leaking that rebinding to any other thread/caller.
			Var nsVar = RT.var("clojure.core", "*ns*");
			Namespace ekekoNs = Namespace.findOrCreate(Symbol.intern("damp.ekeko"));
			Var.pushThreadBindings(RT.map(nsVar, ekekoNs));
			Object result;
			try {
				IFn eval = Clojure.var("clojure.core", "eval");
				result = eval.invoke(form);
			} finally {
				Var.popThreadBindings();
			}

			List<String> vars = extractVars(form);
			List<List<Map<String, Object>>> rows = serializeRows(result);

			Map<String, Object> response = new LinkedHashMap<>();
			response.put("vars", vars != null ? vars : defaultVarNames(rows));
			response.put("rows", rows);
			return response;
		} finally {
			Thread.currentThread().setContextClassLoader(previous);
		}
	}

	/**
	 * The {@code ekeko}/{@code ekeko-n*} macros always take their logic
	 * variables as the first vector literal among their arguments (right
	 * after the solution count, for {@code ekeko-n*}) -- find it generically
	 * off the read form rather than hardcoding per-macro argument positions.
	 */
	private static List<String> extractVars(Object form) {
		if (!(form instanceof Iterable)) {
			return null;
		}
		for (Object element : (Iterable<?>) form) {
			if (element instanceof IPersistentVector) {
				IPersistentVector vector = (IPersistentVector) element;
				List<String> names = new ArrayList<>(vector.count());
				for (int i = 0; i < vector.count(); i++) {
					names.add(String.valueOf(vector.nth(i)));
				}
				return names;
			}
		}
		return null;
	}

	private static List<String> defaultVarNames(List<List<Map<String, Object>>> rows) {
		int width = rows.isEmpty() ? 0 : rows.get(0).size();
		List<String> names = new ArrayList<>(width);
		for (int i = 0; i < width; i++) {
			names.add("Var " + (i + 1));
		}
		return names;
	}

	/**
	 * Converts a plain seq of symbols (e.g. {@code '(?m ?name)}, as passed by
	 * ekeko-star/ekeko-n-star to damp.ekeko.gui/open-barista-results-viewer-star
	 * -- not a full form to search within, unlike {@link #extractVars}) into
	 * column names. Shared with {@link EkekoResultsBridge}.
	 */
	static List<String> symbolNames(Object varsSeq) {
		List<String> names = new ArrayList<>();
		if (varsSeq instanceof Iterable) {
			for (Object v : (Iterable<?>) varsSeq) {
				names.add(String.valueOf(v));
			}
		}
		return names;
	}

	static List<List<Map<String, Object>>> serializeRows(Object result) {
		List<List<Map<String, Object>>> rows = new ArrayList<>();
		if (!(result instanceof Iterable)) {
			return rows;
		}
		for (Object solution : (Iterable<?>) result) {
			List<Map<String, Object>> row = new ArrayList<>();
			if (solution instanceof Iterable) {
				for (Object binding : (Iterable<?>) solution) {
					row.add(serializeCell(binding));
				}
			} else {
				row.add(serializeCell(solution));
			}
			rows.add(row);
		}
		return rows;
	}

	private static Map<String, Object> serializeCell(Object binding) {
        // If the query result is an AST node we serialize it in such a way
        // that an editor could link it back to its source location.
		if (binding instanceof ASTNode) {
			Map<String, Object> location = codeLocation((ASTNode) binding);
			if (location != null) {
				location.put("kind", "code");
				location.put("text", summarize((ASTNode) binding));
				return location;
			}
		}

		Map<String, Object> cell = new LinkedHashMap<>();

		// Ekeko wraps a node's non-ASTNode properties (lists, primitives,
		// absent values) in records that carry only the owner node plus the
		// property descriptor, so their own toString is an object identity
		// and the descriptor's is its name -- neither is the value. Resolve
		// them the same way the Eclipse plugin's Barista view does:
		// SOULLabelProvider.prettyPrint calls astnode/value? and
		// astnode/value-unwrapped (injected as FN_ISWRAPPER/FN_GETWRAPPEDVALUE
		// by damp.ekeko.jdt.baristacallbacks) and renders the unwrapped
		// value's toString. For a list property that yields the JDT
		// ASTNode$NodeList, printing as "[int x, int y]".
		if (isEkekoValueWrapper(binding)) {
			Object unwrapped = unwrapEkekoValue(binding);
			cell.put("kind", "wrapped-value");
			cell.put("text", unwrapped == null ? "null" : String.valueOf(unwrapped));
			return cell;
		}

		cell.put("kind", "value");
		cell.put("text", String.valueOf(binding));
		return cell;
	}

	/** astnode/value? -- true for the list/primitive/absent value wrappers. */
	private static boolean isEkekoValueWrapper(Object binding) {
		return Boolean.TRUE.equals(Clojure.var("damp.ekeko.jdt.astnode", "value?").invoke(binding));
	}

	/** astnode/value-unwrapped -- the wrapper's actual property value. */
	private static Object unwrapEkekoValue(Object wrapper) {
		return Clojure.var("damp.ekeko.jdt.astnode", "value-unwrapped").invoke(wrapper);
	}

	private static String summarize(ASTNode node) {
		String text = node.toString().trim();
		int newline = text.indexOf('\n');
		if (newline >= 0) {
			text = text.substring(0, newline) + " …";
		}
		if (text.length() > 200) {
			text = text.substring(0, 200) + "…";
		}
		return text;
	}

	private static Map<String, Object> codeLocation(ASTNode node) {
		ASTNode root = node.getRoot();
		if (!(root instanceof CompilationUnit)) {
			return null;
		}
		CompilationUnit cu = (CompilationUnit) root;
		IJavaElement javaElement = cu.getJavaElement();
		if (!(javaElement instanceof ICompilationUnit)) {
			return null;
		}
		ICompilationUnit icu = (ICompilationUnit) javaElement;
		IFile file;
		try {
			file = (IFile) icu.getCorrespondingResource();
		} catch (Exception e) {
			return null;
		}
		if (file == null) {
			return null;
		}
		URI uri = file.getLocationURI();
		if (uri == null) {
			return null;
		}
		Map<String, Object> location = new LinkedHashMap<>();
		location.put("uri", uri.toString());
		location.put("startOffset", node.getStartPosition());
		location.put("length", node.getLength());
		return location;
	}
}
