(ns sandbox.ui
  "Chassis alias definitions for UI components.

   Define component structure here, use namespaced attributes for config props:
   - Namespaced attrs (e.g., :card/title) are elided from HTML output
   - Non-namespaced attrs pass through to the element

   Example:
     (defmethod c/resolve-alias ::card
       [_ attrs _]
       (let [{:card/keys [title description]} attrs]
         [:div.card attrs
          [:h3.card-title title]
          [:p.card-description description]]))"
  (:require [dev.onionpancakes.chassis.core :as c]))

;; Example component alias
(defmethod c/resolve-alias ::example-card
  [_ attrs _]
  (let [{:example-card/keys [title description]} attrs]
    [:div.example-card attrs
     [:h3.example-card-title title]
     [:p.example-card-description description]]))
