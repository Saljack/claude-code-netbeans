# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A NetBeans IDE plugin (packaged as a `.nbm`) that makes NetBeans act as an IDE backend for the Claude Code CLI. It implements the same discovery + transport protocol that Claude Code's official IDE integrations use: a lock file under `~/.claude/ide/{port}.lock` and an MCP server over WebSocket. The MCP layer is a **hand-written JSON-RPC implementation** (the MCP Java SDK dependency is intentionally commented out in `pom.xml`).

## Build & test

This module pins **Java 17** (`maven.compiler.source/target=17`, CI builds on JDK 17). Do not assume a newer JDK — the NetBeans Platform deps (`RELEASE290`, i.e. NetBeans 29) and `nbm-maven-plugin` are matched to it.

```bash
mvn clean package          # builds target/claude-code-netbeans-<version>.nbm
mvn clean install nbm:run-ide   # launches a dev NetBeans with the plugin installed
mvn test                   # JUnit 5 (jupiter) + Mockito, via surefire
mvn test -Dtest=CloseTabTest                       # single test class
mvn test -Dtest=CloseTabTest#methodName            # single test method
```

The NetBeans Platform artifacts come from a dedicated repository declared in `pom.xml` (netbeans.osuosl.org), not Maven Central.

## Generated parameter classes (important)

The `tools.params` package does **not** exist in source. Param/result POJOs (e.g. `OpenDiffParams`, `GetWorkspaceFoldersResult`, `Folder`, `Content`) are generated at build time by `jsonschema2pojo` from the JSON Schemas in `src/main/resources/org/openbeans/claude/netbeans/tools/schemas/`, into `target/generated-sources/jsonschema2pojo`. Consequences:

- To change a tool's input/output shape, edit the schema JSON, not a Java class.
- Schemas use snake_case property names (`old_file_path`); `propertyWordDelimiters=_` makes the generated getters camelCase (`getOldFilePath()`).
- After editing a schema, run a build so the matching POJO regenerates before referencing new fields.
- A small number of param/result classes (e.g. under `tools/params/`) are hand-written source — check both the source dir and the schemas dir before assuming where a type lives.

## Architecture & message flow

```
Claude Code CLI --ws--> Jetty (WebSocketMCPServer) --> MCPWebSocketHandler --> NetBeansMCPHandler --> Tool impls --> NetBeans Platform APIs
```

- **`ClaudeCodeInstaller`** (`ModuleInstall`, registered via `META-INF/services/...ModuleInstall`) is the lifecycle entry point. `restored()` starts everything on a `RequestProcessor`; `close()`/`uninstalled()` tears it down and deletes the lock file. It also implements `ClaudeCodeStatusService` (queried by the Tools menu status action and the status-line element) and listens to `OpenProjects` to rewrite the lock file's `workspaceFolders` when projects open/close.
- **`WebSocketMCPServer`** finds a free port in **8990–9100**, starts embedded Jetty, and negotiates the `mcp` / `mcp-v1` subprotocol.
- **`LockFileManager`** writes `~/.claude/ide/{port}.lock` containing `pid`, `ideName`, `transport: "ws"`, a random `authToken`, and `workspaceFolders`. This file is how the CLI discovers the IDE.
- **`MCPWebSocketHandler`** parses each JSON-RPC frame and delegates to `NetBeansMCPHandler.handleMessage`.
- **`NetBeansMCPHandler`** is the protocol core: handles `initialize` (advertises protocolVersion `2024-11-05`, then emits a `notifications/initialized`), `tools/list`, `tools/call`, `resources/*`, `prompts/list`. It also pushes `selection_changed` notifications by tracking caret changes on the active editor's `TopComponent`.

## Adding or changing a tool

Tools implement `Tool<ParamType, ResultType>` (`getName`, `getDescription`, `getParameterClass`, `run`). Registration is **manual and duplicated** in `NetBeansMCPHandler` — to add a tool you must touch all of:

1. A JSON Schema in `tools/schemas/` (becomes the `inputSchema` AND drives POJO generation).
2. A `Tool` implementation in `tools/`.
3. A field + constructor init in `NetBeansMCPHandler`.
4. A `createToolDefinition(...)` line in `handleToolsList()` (the third arg is the schema file basename, no `.json`).
5. A `case` in the `handleToolsCall()` switch.

Tool names are not consistently styled — most are camelCase (`openFile`, `getDiagnostics`) but some are snake_case (`close_tab`). Match the name Claude Code expects rather than enforcing a convention.

### Synchronous vs. asynchronous tools

`run()` either returns a result object (sync) or an `AsyncResponse<O>` (async). When `handleToolsCall` sees an `AsyncResponse`, it returns `null` (no immediate reply), wires an `AsyncHandler`, and the reply is sent later over the WebSocket carrying the **original request id**. `OpenDiff` is the canonical async tool: it opens a diff `TopComponent` with an "Approve" button and resolves only when the user approves (`FILE_SAVED`) or closes the tab. Tab-to-handler wiring lives in the static `DiffTabTracker`; tab-close detection is a `PROP_TC_CLOSED` listener in `NetBeansMCPHandler` that fires `DIFF_REJECTED`.

## Conventions specific to this codebase

- **Security boundary:** every file-touching tool must validate paths with `NbUtils.isPathWithinOpenProjects(...)` (canonicalizes the path and confirms it sits inside an open project dir). Preserve this check when adding file operations.
- **Threading:** UI work (opening editors, diff components) must run on the EDT via `SwingUtilities.invokeLater`; background/server work uses the module's `RequestProcessor`.
- **Line/column indexing is inconsistent by design** to match what each consumer expects: `NbUtils.getCurrentSelectionData` reports **1-based** lines, while the `selection_changed` notification in `NetBeansMCPHandler` reports **0-based** lines/characters. Don't "fix" one to match the other without checking the protocol consumer.
</content>
</invoke>
