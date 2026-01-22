(ns {{top/ns}}.routes
  "Application routes."
  (:require [{{top/ns}}.views.layout :as layout]))

(defn routes
  "Application routes. Kaiin routes merged separately in config."
  []
  [["/" {:get {:handler (fn [_] {:body (layout/home-page)})}}]

   ["/about" {:get {:handler (fn [_] {:body (layout/about-page)})}}]])
