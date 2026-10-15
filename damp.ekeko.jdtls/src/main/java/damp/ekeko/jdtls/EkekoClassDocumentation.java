package damp.ekeko.jdtls;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.ls.core.internal.HoverInfoProvider;
import org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin;
import org.eclipse.jdt.ls.core.internal.javadoc.JavaDoc2MarkdownConverter;
import org.eclipse.lsp4j.MarkedString;

/**
 * The documentation of the classes of the values in a query's results, sent
 * along with them for the results view to show when hovering over a cell.
 * It is the same Markdown the Java editor's hover shows for a class (its
 * signature followed by its Javadoc), computed by jdt.ls's own
 * {@link HoverInfoProvider}.
 *
 * Classes are looked up on the classpath of the workspace's Java projects
 * (e.g. JDK classes, with their Javadoc when their source is available).
 * Those normally don't include JDT's own classes, so the JDT DOM classes
 * (AST nodes and bindings, the values most queries return) are documented
 * from their sources bundled with this bundle instead (see pom.xml), in the
 * same form.
 */
final class EkekoClassDocumentation {

	/** Where the bundled sources are, relative to this class. */
	private static final String BUNDLED_SOURCES = "sources/";

	/** Documentation from the bundled sources, which never changes. */
	private static final Map<String, Optional<String>> BUNDLED_DOCS = new ConcurrentHashMap<>();

	private EkekoClassDocumentation() {
	}

	/**
	 * @param rows serialized results, see
	 *             {@link EkekoQueryRunner#serializeRows}
	 * @return the documentation (as Markdown) of each distinct class of the
	 *         values in {@code rows}, by class name; classes that are not
	 *         found are left out
	 */
	static Map<String, String> classDocs(List<List<Map<String, Object>>> rows) {
		Map<String, String> docs = new LinkedHashMap<>();
		Map<String, Boolean> seen = new LinkedHashMap<>();
		for (List<Map<String, Object>> row : rows) {
			for (Map<String, Object> cell : row) {
				Object className = cell.get("className");
				if (className == null || seen.put((String) className, Boolean.TRUE) != null) {
					continue;
				}
				try {
					String doc = classDocumentation((String) className);
					if (doc != null) {
						docs.put((String) className, doc);
					}
				} catch (Exception e) {
					// Missing documentation must not keep the results from showing.
					JavaLanguageServerPlugin.logException("Ekeko: no documentation for " + className, e);
				}
			}
		}
		return docs;
	}

	private static String classDocumentation(String className) throws Exception {
		IType type = findType(className);
		if (type == null) {
			return BUNDLED_DOCS.computeIfAbsent(className, name -> Optional.ofNullable(bundledDocumentation(name)))
					.orElse(null);
		}
		MarkedString signature = HoverInfoProvider.computeSignature(type);
		MarkedString javadoc = HoverInfoProvider.computeJavadoc(type);
		return hoverMarkdown(signature == null ? null : signature.getValue(),
				javadoc == null ? null : javadoc.getValue());
	}

	/**
	 * Lays out documentation like the Java editor's hover: the signature as
	 * a Java code block, then the Javadoc below a horizontal rule.
	 */
	private static String hoverMarkdown(String signature, String javadoc) {
		StringBuilder markdown = new StringBuilder();
		if (signature != null) {
			markdown.append("```java\n").append(signature).append("\n```");
		}
		if (javadoc != null && !javadoc.isBlank()) {
			markdown.append("\n\n---\n\n").append(javadoc);
		}
		return markdown.toString();
	}

	/**
	 * The documentation of a class from its bundled source, or {@code null}
	 * when its source isn't bundled. The Javadoc is converted to Markdown
	 * the same way jdt.ls converts it for its own hovers.
	 */
	private static String bundledDocumentation(String className) {
		String[] names = className.split("\\$");
		String source = bundledSource(names[0]);
		if (source == null) {
			return null;
		}
		ASTParser parser = ASTParser.newParser(AST.getJLSLatest());
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(source.toCharArray());
		CompilationUnit unit = (CompilationUnit) parser.createAST(null);
		AbstractTypeDeclaration type = findTypeDeclaration(unit.types(), simpleName(names[0]));
		for (int i = 1; type != null && i < names.length; i++) {
			type = findTypeDeclaration(type.bodyDeclarations(), names[i]);
		}
		if (type == null) {
			return null;
		}
		String javadoc = null;
		Javadoc comment = type.getJavadoc();
		if (comment != null) {
			String text = source.substring(comment.getStartPosition(),
					comment.getStartPosition() + comment.getLength());
			try {
				javadoc = new JavaDoc2MarkdownConverter(commentContents(text)).getAsString();
			} catch (Exception e) {
				JavaLanguageServerPlugin.logException("Ekeko: could not convert the Javadoc of " + className, e);
			}
		}
		return hoverMarkdown(className.replace('$', '.'), javadoc);
	}

	private static String bundledSource(String topLevelClassName) {
		String path = BUNDLED_SOURCES + topLevelClassName.replace('.', '/') + ".java";
		try (InputStream in = EkekoClassDocumentation.class.getResourceAsStream(path)) {
			return in == null ? null : new String(in.readAllBytes(), StandardCharsets.UTF_8);
		} catch (Exception e) {
			JavaLanguageServerPlugin.logException("Ekeko: could not read " + path, e);
			return null;
		}
	}

	private static AbstractTypeDeclaration findTypeDeclaration(List<?> declarations, String simpleName) {
		for (Object declaration : declarations) {
			if (declaration instanceof AbstractTypeDeclaration
					&& ((AbstractTypeDeclaration) declaration).getName().getIdentifier().equals(simpleName)) {
				return (AbstractTypeDeclaration) declaration;
			}
		}
		return null;
	}

	private static String simpleName(String qualifiedName) {
		return qualifiedName.substring(qualifiedName.lastIndexOf('.') + 1);
	}

	/**
	 * The text of a Javadoc comment without its delimiters and the leading
	 * {@code *} of each line, as jdt.ls's Javadoc converters expect it.
	 */
	private static String commentContents(String comment) {
		String body = comment.substring(3, comment.length() - 2);
		StringBuilder contents = new StringBuilder();
		for (String line : body.split("\\R", -1)) {
			String stripped = line.stripLeading();
			if (stripped.startsWith("*")) {
				stripped = stripped.substring(1);
				if (stripped.startsWith(" ")) {
					stripped = stripped.substring(1);
				}
				line = stripped;
			}
			contents.append(line).append('\n');
		}
		return contents.toString();
	}

	private static IType findType(String className) throws Exception {
		// IJavaProject.findType expects member types separated by '.', not by
		// the '$' of their binary name.
		String name = className.replace('$', '.');
		for (IJavaProject project : JavaCore.create(ResourcesPlugin.getWorkspace().getRoot()).getJavaProjects()) {
			IType type = project.findType(name);
			if (type != null) {
				return type;
			}
		}
		return null;
	}
}
