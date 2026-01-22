(ns {{top/ns}}.routes
  "Application routes."
  (:require [{{top/ns}}.views.layout :as layout]
            [ascolais.sfere :as sfere]
            [ascolais.twk :as twk]))

(defn sse-home
  "Establish persistent SSE connection for home page.
   This is a manual handler (not kaiin) because it keeps the connection open."
  [{:keys [signals]}]
  (let [session-id (:sessionId signals)]
    {::sfere/key [:page "home" session-id]
     ::twk/fx [[::twk/patch-signals {:connected true}]]}))

(defn routes
  "Application routes. Kaiin routes merged separately in config."
  []
  [["/" {:get {:handler (fn [_] {:body (layout/home-page)})}}]

   ["/about" {:get {:handler (fn [_] {:body (layout/about-page)})}}]

   ["/sse/home" {:get {:handler sse-home}}]])
