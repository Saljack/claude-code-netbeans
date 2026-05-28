# Claude Code NetBeans Plugin

> **Looking for maintainers:** Due to my current job situation, I'm no longer able to actively contribute to this project. The plugin is essentially unmaintained and needs help. Contributions and new maintainers are very welcome!

A NetBeans IDE plugin that provides integration with Claude Code through the Model Context Protocol (MCP).

![Downloads](https://img.shields.io/endpoint?url=https://openbeans.org/plugin-counter/api/118)

## What's new in 2.0

- **Embedded terminal**: open `Tools > Open Claude Terminal` and the `claude` CLI launches inside a dockable NetBeans tab, backed by [JediTerm](https://github.com/JetBrains/jediterm) + [pty4j](https://github.com/JetBrains/pty4j). The child process auto-binds to this NetBeans instance (via `CLAUDE_CODE_SSE_PORT` + `ENABLE_IDE_INTEGRATION`), so MCP tools such as `openDiff` and `getDiagnostics` work without any external configuration.
- **Java 21** baseline (was 17).

## Features

- **Embedded `claude` CLI**: Dockable JediTerm-backed terminal that auto-launches and auto-binds to this NetBeans instance
- **Automatic Detection**: Creates a lock file that Claude Code CLI can discover
- **WebSocket Communication**: Establishes real-time communication using MCP over WebSocket
- **IDE Integration**: Provides access to NetBeans project structure, file operations, and editor content
- **File Operations**: Read, write, and list files through Claude Code
- **Project Management**: Access open projects and project files
- **Document Access**: Retrieve content from open documents in the editor

## Installation

### Prerequisites

- NetBeans IDE 29.0 or later
- Java 21 or later (the plugin module is built against Java 21)
- Maven 3.6 or later
- `claude` CLI on `PATH` (override with `-J-Dclaude.code.cli=/path/to/claude` in `etc/netbeans.conf`)

### Building the Plugin

1. Clone or download this project
2. Navigate to the project directory
3. Build the plugin:

```bash
mvn clean package
```

4. The plugin will be built as `target/claude-code-netbeans-2.0.0.nbm`

> **Repository access:** the 2.0 build pulls JediTerm 3.x from JetBrains' `intellij-dependencies` repo (declared in `pom.xml`). If your `~/.m2/settings.xml` mirrors all repos through a corporate Nexus/Artifactory, ensure that mirror proxies `https://packages.jetbrains.team/maven/p/ij/intellij-dependencies` or whitelist this repo (e.g. `<mirrorOf>*,!intellij-dependencies</mirrorOf>`).

### Third-party libraries

The 2.0 release bundles:

- [JediTerm](https://github.com/JetBrains/jediterm) (LGPL-3.0) and [pty4j](https://github.com/JetBrains/pty4j) (EPL-1.0) — terminal widget and PTY backend
- Kotlin stdlib (Apache-2.0) — runtime dependency of JediTerm 3.x

### Installing in NetBeans

1. Open NetBeans IDE
2. Go to **Tools > Plugins**
3. Click the **Downloaded** tab
4. Click **Add Plugins...** and select the `.nbm` file
5. Follow the installation wizard
6. Restart NetBeans when prompted

## Usage

### Automatic Startup

The plugin automatically starts when NetBeans launches and:

1. **Creates Lock File**: Writes connection information to `~/.claude/ide/{port}.lock`
2. **Starts WebSocket Server**: Listens on an available port (8990-9100 range)
3. **Updates on Changes**: Refreshes workspace information when projects are opened/closed

### Using with Claude Code

1. **Install Claude Code**: Follow the [official installation guide](https://docs.anthropic.com/en/docs/claude-code/overview)

2. **Start NetBeans**: Open NetBeans with your project

3. **Run Claude Code**: In any terminal, run:
   ```bash
   claude
   ```

4. **Verify Connection**: Claude Code should automatically detect NetBeans. You can verify with:
   ```bash
   /ide
   ```

### Available MCP Tools

The plugin provides these tools to Claude Code:

#### File Operations
- `read_file`: Read file contents
- `write_file`: Write content to files
- `list_files`: List directory contents

#### Project Operations
- `get_open_projects`: List all open projects
- `get_project_files`: Get files in a specific project

#### Editor Operations
- `get_open_documents`: List open documents
- `get_document_content`: Get content from open documents

### Plugin Status

Check the plugin status through **Tools > Claude Code Status** in the NetBeans menu.

## Architecture

### Components

1. **ClaudeCodeInstaller**: Main plugin lifecycle manager
2. **LockFileManager**: Handles lock file creation and updates
3. **WebSocketMCPServer**: WebSocket server for MCP communication
4. **NetBeansMCPHandler**: Processes MCP messages and provides IDE capabilities
5. **MCPWebSocketHandler**: WebSocket message routing

### Communication Flow

```
Claude Code CLI → WebSocket → NetBeans Plugin → NetBeans IDE APIs
                ←           ←                  ←
```

### Lock File Format

```json
{
  "pid": 12345,
  "ideName": "NetBeans",
  "transport": "ws",
  "port": 8991,
  "workspaceFolders": ["/path/to/project"]
}
```

## Development

### Project Structure

```
src/main/java/org/openbeans/claude/netbeans/
├── ClaudeCodeInstaller.java      # Plugin lifecycle
├── LockFileManager.java          # Lock file management
├── WebSocketMCPServer.java       # WebSocket server
├── MCPWebSocketHandler.java      # WebSocket message handler
├── NetBeansMCPHandler.java       # MCP protocol implementation
└── ClaudeCodeAction.java         # Status action

src/main/resources/
├── org/openbeans/claude/netbeans/Bundle.properties
└── META-INF/services/org.openide.modules.ModuleInstall

src/main/nbm/
└── manifest.mf                   # Plugin manifest
```

### Dependencies

- **NetBeans Platform APIs**: IDE integration
- **Model Context Protocol SDK**: MCP implementation
- **Jetty WebSocket**: WebSocket server
- **Jackson**: JSON processing

### Building for Development

```bash
# Build and install in development NetBeans
mvn clean install nbm:run-ide

# Package for distribution
mvn clean package
```

## Troubleshooting

### Plugin Not Loading
- Check NetBeans logs: **View > IDE Log**
- Verify Java 17+ is being used
- Ensure all dependencies are available

### Claude Code Not Connecting
- Verify lock file exists: `~/.claude/ide/{port}.lock`
- Check if WebSocket port is accessible
- Review plugin status: **Tools > Claude Code Status**

### WebSocket Connection Issues
- Check firewall settings for the port range (8990-9100)
- Verify no other applications are using the ports
- Review NetBeans and plugin logs

## Contributing

Contributions are very welcome! Feel free to:

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## License

See the LICENSE file for details.

## Support

For issues and questions:
- Check the NetBeans IDE logs
- Review Claude Code documentation
- Create an issue in the project repository
