(ns {{top/ns}}.{{main}}
  "Application entry point."
  (:require [{{top/ns}}.config :as config]
            [integrant.core :as ig]))

(defn -main
  "Application entry point for production."
  [& _args]
  (println "Starting {{name}}...")
  (ig/init config/config)
  (println "{{name}} started on port 3000"))
