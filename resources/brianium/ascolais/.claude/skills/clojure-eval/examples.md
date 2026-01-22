# Clojure REPL Evaluation Examples

## Basic Evaluation

```bash
# Simple expression
clj-nrepl-eval -p 7888 "(+ 1 2 3)"

# Define a function
clj-nrepl-eval -p 7888 "(defn greet [name] (str \"Hello, \" name \"!\"))"

# Call the function
clj-nrepl-eval -p 7888 "(greet \"World\")"
```

## Namespace Management

```bash
# Switch to dev namespace
clj-nrepl-eval -p 7888 "(dev)"

# Reload changed namespaces
clj-nrepl-eval -p 7888 "(reload)"

# Require with reload
clj-nrepl-eval -p 7888 "(require '[{{top/ns}}.config :as config] :reload)"
```

## System Lifecycle

```bash
# Start the system
clj-nrepl-eval -p 7888 "(start)"

# Stop the system
clj-nrepl-eval -p 7888 "(stop)"

# Restart (stop + reload + start)
clj-nrepl-eval -p 7888 "(restart)"
```

## Effect Discovery

```bash
# List all effects
clj-nrepl-eval -p 7888 "(s/describe (dispatch))"

# Inspect specific effect
clj-nrepl-eval -p 7888 "(s/describe (dispatch) :ascolais.twk/patch-elements)"

# Generate sample invocation
clj-nrepl-eval -p 7888 "(s/sample (dispatch) :ascolais.tsain/preview)"

# Search effects by pattern
clj-nrepl-eval -p 7888 "(s/grep (dispatch) \"broadcast\")"
```

## Component Preview

```bash
# Preview a component
clj-nrepl-eval -p 7888 "(dispatch [[:ascolais.tsain/preview [:h1 \"Hello World\"]]])"

# Clear preview
clj-nrepl-eval -p 7888 "(dispatch [[:ascolais.tsain/preview-clear]])"

# Append to preview
clj-nrepl-eval -p 7888 "(dispatch [[:ascolais.tsain/preview-append [:div.card \"Card 1\"]]])"
```

## Database Migrations

```bash
# Apply pending migrations
clj-nrepl-eval -p 7888 "(migrate!)"

# Roll back last migration
clj-nrepl-eval -p 7888 "(rollback!)"
```

## Multiline Code with Heredoc

```bash
clj-nrepl-eval -p 7888 "$(cat <<'EOF'
(defn complex-fn [data]
  (->> data
       (filter :active)
       (map :name)
       (sort)))
EOF
)"
```
