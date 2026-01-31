# Move Public to Resources - Research

## Problem Statement

Currently the template has:

```
dev/resources/
  components.edn       # Tsain component library
  public/
    styles.css         # Component CSS (hot-reloadable)

resources/
  migrations/          # SQL migration files
```

This creates issues:

1. **File watcher path mismatch**: The file watcher is configured to watch `["resources/public"]`, but styles.css lives at `dev/resources/public/styles.css`

2. **Non-standard location**: Static assets conventionally live in `resources/public/` which is on the classpath

3. **Mixed concerns**: `dev/resources/` contains both dev-only config and production assets

## Requirements

### Functional Requirements

1. Move styles.css to resources/public/
2. Update tsain.edn stylesheet path
3. Update documentation references

### Non-Functional Requirements

- No behavior change for static file serving
- File watcher continues to work

## Target Structure

```
dev/resources/
  components.edn       # Tsain component library (dev-only)

resources/
  migrations/          # SQL migration files
  public/
    styles.css         # Component CSS (hot-reloadable)
```

## Technical Notes

1. **Ring middleware**: The `wrap-resource "public"` middleware looks for a `public/` directory on the classpath. Since `resources/` is always on the classpath, `resources/public/styles.css` becomes available at `/styles.css`

2. **File watcher**: The beholder file watcher uses filesystem paths relative to the working directory. `["resources/public"]` will correctly watch `resources/public/`

3. **Tsain configuration**: Tsain reads the `:stylesheet` path to know which CSS file to reload. This is a filesystem path, so it should be `resources/public/styles.css`

## Affected Files

- `resources/brianium/ascolais/resources/public/styles.css` (NEW)
- `resources/brianium/ascolais/dev/resources/public/` (DELETE)
- `resources/brianium/ascolais/build/tsain.edn` (update path)
- `resources/brianium/ascolais/build/CLAUDE.md` (update docs)
- `resources/brianium/ascolais/build/README.md` (update docs)
- `resources/brianium/ascolais/.claude/skills/component-iterate/SKILL.md` (update docs)
- `README.md` (update docs)
