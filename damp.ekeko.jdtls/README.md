# damp.ekeko.jdtls

Runs Ekeko's query engine (reused from `damp.ekeko.plugin`) inside a running
Eclipse JDT Language Server (`eclipse.jdt.ls`) process, as an OSGi bundle
contributed via the `contributes.javaExtensions` mechanism used by VS Code
extensions such as `redhat.java`'s companions (`vscode-java-test`,
`vscode-java-debug`).

Unlike `damp.ekeko.plugin`, this module is not built with Tycho: it doesn't
target a fixed Eclipse release, but whatever JDT/Eclipse Platform version the
currently installed `redhat.java` extension bundles. It depends on those
packages as ordinary Maven dependencies (Eclipse Platform and JDT publish
their releases to Maven Central under matching coordinates/versions), and on
Clojure/core.logic/qwal, which get embedded into the bundle jar since jdt.ls
doesn't provide them.

## Building

Two dependencies aren't published to any public repository and must be
installed into the local Maven repository first:

```sh
./install-local-deps.sh
```

This installs `org.eclipse.jdt.ls:org.eclipse.jdt.ls.core` (extracted from
the currently installed `redhat.java` extension) and `damp:qwal` (from
`damp.ekeko/damp.libs/libs/qwal-1.0.3-SNAPSHOT.jar`). Re-run it whenever the
installed `redhat.java` version changes.

Then build normally:

```sh
mvn verify
```

This produces `target/damp.ekeko.jdtls-<version>.jar`, an OSGi bundle ready
to be contributed to a running jdt.ls instance via the `bundles`
initialization option (or `java.reloadBundles`).

## Source layout

`src/main/java` holds this module's own code (currently just
`EkekoJdtlsActivator`, the `BundleActivator` that boots Ekeko inside jdt.ls).
The rest of the query engine is built directly from `damp.ekeko.plugin`'s
own source (see the `maven-resources-plugin`/`build-helper-maven-plugin`
configuration in `pom.xml`), excluding the Eclipse-workbench-coupled parts
of that plugin that don't apply here (the Barista results-table UI,
property/preference pages, menu action handlers, the REPL controller, and
the Soot/workspace-reification subtrees).
