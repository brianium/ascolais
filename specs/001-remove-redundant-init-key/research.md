# Remove Redundant init-key Defmethods - Research

## Problem Statement

The current template defines initializer functions and then wraps them in defmethods:

```clojure
;; Function already exists
(defn datasource [{:keys [jdbc-url username password]}]
  ...)

;; This defmethod is redundant
(defmethod ig/init-key ::datasource [_ opts]
  (datasource opts))
```

Integrant will automatically find `datasource` for the `::datasource` key if the namespace is loaded, making these defmethods unnecessary boilerplate.

## Requirements

### Functional Requirements

1. Remove init-key defmethods that simply delegate to same-named functions
2. Keep halt-key! defmethods (required for resource cleanup)
3. Ensure system still initializes and halts correctly

### Non-Functional Requirements

- No behavior change from user perspective
- Cleaner, more idiomatic Integrant usage

## Affected Files Analysis

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

2. **Namespace loading**: Integrant must load the namespaces containing initializer functions. Verify that `ig/load-namespaces` is called on the config before `ig/init`

3. **Section cleanup**: After removing the init-key defmethods, the "Integrant Methods" sections can be renamed to "Integrant Halt Methods" or similar

## References

- [Integrant Initializer Functions](https://github.com/weavejester/integrant?tab=readme-ov-file#initializer-functions)
