# 001: Remove Redundant init-key Defmethods

**Status:** Complete

## Summary

Remove redundant `defmethod ig/init-key` implementations that merely delegate to same-named initializer functions. Integrant's [initializer functions](https://github.com/weavejester/integrant?tab=readme-ov-file#initializer-functions) feature automatically discovers and uses functions matching the keyword name, making the explicit defmethods unnecessary.

## Background

The current template defines initializer functions and then wraps them in defmethods:

```clojure
;; Function already exists
(defn datasource [{:keys [jdbc-url username password]}]
  ...)

;; This defmethod is redundant
(defmethod ig/init-key ::datasource [_ opts]
  (datasource opts))
```

Integrant will automatically find `datasource` for the `::datasource` key if the namespace is loaded.

## Affected Files

### `resources/brianium/ascolais/src/clj/config.clj`

Remove these init-key defmethods (lines 72-91):
- `::datasource` - function `datasource` exists at line 22
- `::store` - function `store` exists at line 32
- `::dispatch` - function `dispatch` exists at line 37
- `::router` - function `router` exists at line 46
- `::handler` - function `handler` exists at line 55
- `::server` - function `server` exists at line 62

**Keep** these halt-key defmethods:
- `::datasource` halt-key! (line 75) - closes HikariDataSource
- `::server` halt-key! (line 93) - calls stop function

### `resources/brianium/ascolais/dev/src/clj/dev/config.clj`

Remove these init-key defmethods (lines 48-58):
- `::tsain-registry` - function `tsain-registry` exists at line 21
- `::file-watcher` - function `file-watcher` exists at line 26
- `::router` - function `dev-router` exists at line 34 (**requires rename to `router`**)

**Keep** this halt-key defmethod:
- `::file-watcher` halt-key! (line 54) - stops beholder watcher

### `resources/brianium/ascolais/src/clj/fx/example.clj`

Remove this init-key defmethod (line 41):
- `::registry` - function `registry` exists at line 8

## Implementation Notes

1. **Function naming**: The `dev-router` function must be renamed to `router` to match the `::router` key

2. **Namespace loading**: Integrant must load the namespaces containing initializer functions. Verify that `ig/load-namespaces` is called on the config before `ig/init`. Check the user.clj/dev.clj setup.

3. **Section cleanup**: After removing the init-key defmethods, the "Integrant Methods" sections can be renamed to "Integrant Halt Methods" or similar, containing only halt-key! implementations. Files with no halt-key! methods (like fx/example.clj) can remove the section entirely.

## Verification

After changes:
1. Start a generated project's REPL
2. Run `(start)` - system should initialize all components
3. Run `(restart)` - system should halt and reinitialize cleanly
4. Verify datasource closes properly (no connection pool warnings)
5. Verify server stops properly (port released)
