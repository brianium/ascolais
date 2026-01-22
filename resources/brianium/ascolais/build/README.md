# {{name}}

A Clojure web application powered by the sandestin effect ecosystem.

## Quick Start

### Prerequisites

- Java 21+
- Clojure CLI tools
- Docker (for PostgreSQL)

### Development

```bash
# Start the database
docker-compose up -d

# Start REPL
clj -M:dev

# In the REPL:
(dev)      ; Load dev namespace
(start)    ; Start server at localhost:3000
(stop)     ; Stop server
(reload)   ; Reload changed namespaces
(restart)  ; Full stop + reload + start
```

### Database Migrations

```clojure
(migrate!)   ; Apply pending migrations
(rollback!)  ; Undo last migration
```

### Tests

```bash
clj -X:test
```

## Technology Stack

| Library | Purpose |
|---------|---------|
| **sandestin** | Effect dispatch with schema-driven discoverability |
| **twk** | Datastar SSE integration |
| **sfere** | Connection management and broadcasting |
| **kaiin** | Declarative HTTP routing from registry metadata |
| **manse** | Database effects with next.jdbc |
| **tsain** | REPL-driven component development sandbox |

## Project Structure

```
src/clj/{{top/file}}/
  {{main/file}}.clj    # Application entry point
  config.clj           # Integrant system configuration
  routes.clj           # Ring route handlers
  fx/                  # Effect registries (one per domain)

dev/src/clj/
  user.clj             # REPL initialization
  dev.clj              # Dev namespace (start/stop/dispatch)
  dev/config.clj       # Dev system config (extends app)
  sandbox/
    ui.clj             # Chassis alias definitions
    views.clj          # Sandbox view re-exports

resources/
  components.edn       # Tsain component library
  migrations/          # SQL migration files

dev/resources/public/
  styles.css           # Component CSS (hot-reloadable)
```

## Claude Code Integration

This project is optimized for Claude-assisted development. See `CLAUDE.md` for comprehensive documentation on the effect system, Datastar patterns, and development workflows.

The `.claude/` directory contains:
- Paren repair hooks for Clojure formatting
- Skills for REPL evaluation and component iteration
