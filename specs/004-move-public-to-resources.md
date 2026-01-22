# 004: Move dev/resources/public to resources/public

**Status:** Active

## Summary

Move the `dev/resources/public/` directory (containing `styles.css`) to the template's root `resources/public/` directory. This consolidates static assets in the standard location and fixes the file watcher path mismatch. After this change, `dev/resources/` should only contain `components.edn`.

## Background

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

1. **File watcher path mismatch**: The file watcher is configured to watch `["resources/public"]`, but styles.css lives at `dev/resources/public/styles.css`. These paths don't match.

2. **Non-standard location**: Static assets conventionally live in `resources/public/` which is on the classpath. The current setup requires `dev/resources` to be added to paths for the middleware to find them.

3. **Mixed concerns**: `dev/resources/` contains both dev-only config (`components.edn`) and assets that could serve in production (`styles.css`).

## Target Structure

```
dev/resources/
  components.edn       # Tsain component library (dev-only)

resources/
  migrations/          # SQL migration files
  public/
    styles.css         # Component CSS (hot-reloadable)
```

## Affected Files

### `resources/brianium/ascolais/resources/public/styles.css` (NEW)

Move the contents of `dev/resources/public/styles.css` to this new location.

### `resources/brianium/ascolais/dev/resources/public/` (DELETE)

Remove the entire `public/` directory from `dev/resources/`.

### `resources/brianium/ascolais/build/tsain.edn`

Update stylesheet path:

```clojure
;; Before
{:ui-namespace sandbox.ui
 :components-file "dev/resources/components.edn"
 :stylesheet "dev/resources/public/styles.css"
 :port 3000}

;; After
{:ui-namespace sandbox.ui
 :components-file "dev/resources/components.edn"
 :stylesheet "resources/public/styles.css"
 :port 3000}
```

### `resources/brianium/ascolais/dev/src/clj/dev/config.clj`

The file watcher path `["resources/public"]` is already correct for the new location. No change needed.

The `wrap-resource "public"` middleware serves from classpath `public/`, which maps to `resources/public/` since `resources/` is on the classpath. No change needed.

### `resources/brianium/ascolais/build/CLAUDE.md`

Update the Project Structure section (around line 182):

```markdown
;; Before
dev/resources/
  components.edn         # Tsain component library
  public/
  styles.css             # Component CSS (hot-reloadable)

;; After
dev/resources/
  components.edn         # Tsain component library (dev-only)

resources/
  migrations/            # SQL migration files
  public/
    styles.css           # Component CSS (hot-reloadable)
```

Update the "Component Styling Conventions" section (around line 567):

```markdown
;; Before
2. **Before commit** - Extract all styles to `dev/resources/public/styles.css`

;; After
2. **Before commit** - Extract all styles to `resources/public/styles.css`
```

### `resources/brianium/ascolais/build/README.md`

Update Project Structure section (around line 74-77):

```markdown
;; Before
dev/resources/
  components.edn       # Tsain component library
  public/
    styles.css         # Component CSS (hot-reloadable)

;; After
dev/resources/
  components.edn       # Tsain component library (dev-only)

resources/
  migrations/          # SQL migration files
  public/
    styles.css         # Component CSS (hot-reloadable)
```

### `resources/brianium/ascolais/.claude/skills/component-iterate/SKILL.md`

Update the configuration example (around line 18):

```markdown
;; Before
 :stylesheet "dev/resources/public/styles.css"  ;; CSS for hot reload

;; After
 :stylesheet "resources/public/styles.css"  ;; CSS for hot reload
```

### `README.md` (ascolais project root)

Update the documentation reference (around line 199):

```markdown
;; Before
CSS hot-reloads automatically when editing `dev/resources/public/styles.css`.

;; After
CSS hot-reloads automatically when editing `resources/public/styles.css`.
```

## Implementation Notes

1. **Ring middleware**: The `wrap-resource "public"` middleware looks for a `public/` directory on the classpath. Since `resources/` is always on the classpath, `resources/public/styles.css` becomes available at `/styles.css`.

2. **File watcher**: The beholder file watcher uses filesystem paths relative to the working directory. `["resources/public"]` will correctly watch `resources/public/` for changes.

3. **Tsain configuration**: Tsain reads the `:stylesheet` path to know which CSS file to reload. This is a filesystem path, so it should be `resources/public/styles.css`.

4. **components.edn stays in dev/resources**: This file is dev-only (component library for the sandbox) and should remain in `dev/resources/` which is only on the dev classpath.

## Verification

After changes:

1. Generate a new project from the template
2. Start the REPL with `clj -M:dev`
3. Run `(dev)` then `(start)`
4. Open `localhost:3000/sandbox` in a browser
5. Edit `resources/public/styles.css` and verify hot-reload works
6. Verify static assets are served (e.g., `/styles.css` returns the CSS file)
7. Confirm `dev/resources/` only contains `components.edn`
