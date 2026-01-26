(ns {{top/ns}}.views.components
  "Chassis alias definitions for reusable UI components.

   Components use html.yeah/defelem for schema-validated, self-documenting aliases:
   - Malli schema defines required/optional props with types
   - :doc metadata provides component description for discovery
   - :<component>/keys for namespaced destructuring
   - Namespaced attrs are elided from HTML output
   - Non-namespaced attrs (classes, data-* attributes) pass through

   Example:
     (hy/defelem card
       [:map {:doc \"A card with title and description\"
              :card/keys [title description]}
        [:card/title :string]
        [:card/description {:optional true} :string]]
       [:div.card attrs
        [:h3.card-title card/title]
        (when card/description
          [:p.card-description card/description])])

   Discovery API (in dev namespace):
     (tsain/describe)                    ; List all components
     (tsain/describe ::example-card)     ; Inspect specific component
     (tsain/grep \"card\")                 ; Search by keyword"
  (:require [html.yeah :as hy]))

(hy/defelem example-card
  [:map {:doc "A simple card component demonstrating the defelem pattern.
               Displays a title and description with themed styling."
         :example-card/keys [title description]}
   [:example-card/title :string]
   [:example-card/description :string]]
  [:div.example-card attrs
   [:h3.example-card-title example-card/title]
   [:p.example-card-description example-card/description]])
