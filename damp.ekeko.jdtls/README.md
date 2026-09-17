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

`src/main/java` holds this module's own code:

- `EkekoJdtlsActivator` -- the `BundleActivator` that boots Ekeko inside
  jdt.ls (enables the Ekeko nature on open Java projects).
- `EkekoCommandHandler` -- the `IDelegateCommandHandler` dispatching
  `workspace/executeCommand` calls (`ekeko.rebuildModel`, `ekeko.startRepl`,
  `ekeko.stopRepl`), registered via `src/main/resources/plugin.xml`.
- `EkekoReplController` -- starts/stops a plain nREPL server inside this
  bundle's own classloader, for a client (e.g. Calva) to connect to.

The rest of the query engine is built directly from `damp.ekeko.plugin`'s
own source (see the `maven-resources-plugin`/`build-helper-maven-plugin`
configuration in `pom.xml`), excluding the Eclipse-workbench-coupled parts
of that plugin that don't apply here (the Barista results-table UI,
property/preference pages, menu action handlers, the REPL controller, and
the Soot/workspace-reification subtrees).

## Troubleshooting: stale Equinox bundle cache

Equinox (the OSGi runtime jdt.ls runs on) persists a bundle cache under
`<redhat.java's globalStorage config dir>/org.eclipse.osgi/`, separate from
the per-workspace `-data` directory. When iterating on this module locally,
symptoms like a bundle silently not exposing its `plugin.xml` extensions (no
warning, or "The bundle is not marked as singleton" even after fixing that),
or a `FileNotFoundException` for a resource that's verifiably present in the
jar (e.g. `nrepl/server.clj`) with the exact same classloader on both sides,
are usually this cache holding onto a bundle registration from a previous,
different version of the jar at the same install path. Deleting that
`org.eclipse.osgi` directory (both `config_linux` and `config_ss_linux`, or
their platform equivalents) and restarting resolves it; clearing just the
`-data`/workspaceStorage directory is not enough.
