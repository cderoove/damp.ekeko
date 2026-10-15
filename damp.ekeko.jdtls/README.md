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

Dependencies are resolved automatically by Maven:
- `org.eclipse.jdt.ls:org.eclipse.jdt.ls.core` is fetched from the Eclipse Releases repository (`repo.eclipse.org`).
- `damp:qwal` is resolved from the in-tree repository at `damp.libs/repo`.

Build normally with Maven:

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
  `ekeko.stopRepl`, `ekeko.runQuery`, `ekeko.cancelQuery`), registered via
  `src/main/resources/plugin.xml`.
- `EkekoReplController` -- starts/stops an nREPL server (with cider-nrepl's
  middleware) inside this bundle's own classloader, for a client (e.g.
  Calva) to connect to. The server itself is managed by
  `src/main/resources/damp/ekeko/nrepl.clj`. `ekeko.startRepl` takes an
  optional eval timeout in milliseconds as its first argument; evaluations
  running longer are interrupted (`damp/ekeko/middleware.clj`).
- `EkekoQueryRunner` -- backs `ekeko.runQuery`: evals a query string in the
  `damp.ekeko` namespace and serializes the result into plain
  `Map`/`List`/primitives (Gson-friendly), turning any `ASTNode` binding
  into a `{kind: "code", uri, startOffset, length}` cell the VS Code side
  can jump to, and everything else into a plain value cell.
- `EkekoResultsBridge` -- called from this bundle's own
  `src/main/resources/damp/ekeko/gui.clj` (see below), pushes a query's
  serialized results to the VS Code extension host as a `workspace/notify`
  notification (`JavaClientConnection.sendNotification`), which
  `redhat.java`'s bundled LSP client dispatches as an ordinary
  `vscode.commands.executeCommand("ekeko.showResults", ...)`.

`src/main/resources/damp/ekeko/gui.clj` is this bundle's own stand-in for
`damp.ekeko.plugin`'s SWT-based `damp.ekeko.gui` (excluded here -- see
`pom.xml` -- since it `:import`s SWT/JFace/baristaui classes at namespace-
load time, none of which are present in jdt.ls's headless runtime).
`damp/ekeko.clj` requires whichever `damp.ekeko.gui` its host supplies, and
`ekeko*`/`ekeko-n*` call its `with-query-progress`,
`eclipse-uithread-return` and `open-barista-results-viewer*`. This bundle's
version notifies the VS Code extension through `EkekoResultsBridge`: when a
query starts and finishes (`ekeko.queryStarted`/`ekeko.queryFinished`, shown
as a progress notification), and with its results (`ekeko.showResults`).
`ekeko.cancelQuery` (given the id from `ekeko.queryStarted`) interrupts the
nREPL evaluation running that query, e.g. from the progress notification's
cancel button.
This means evaluating `(ekeko* ...)`/`(ekeko-n* ...)` at a REPL connected
here pops the VS Code results view automatically, same as it pops Barista's
view in the Eclipse plugin.

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
