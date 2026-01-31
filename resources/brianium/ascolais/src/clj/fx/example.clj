(ns {{top/ns}}.fx.example
  "Example domain effects."
  (:require [ascolais.sandestin :as s]
            [ascolais.twk :as twk]
            [ascolais.sfere :as sfere]))

(defn registry
  "Example effects registry."
  [{:keys [_datasource]}]
  {::s/actions
   {::greet
    {::s/description "Greet a user by name."
     ::s/schema [:tuple [:= ::greet] :string]
     ::s/handler
     (fn [_state name]
       ;; Broadcast greeting to all home page connections
       [[::sfere/broadcast {:pattern [:* [:page "home" :*]]}
         [::twk/patch-elements
          [:div#greeting.greeting
           [:h2 "Hello, " name "!"]
           [:p "Welcome to {{name}}"]]]]])}}

   ::s/effects
   {::log
    {::s/description "Log a message to the console."
     ::s/schema [:tuple [:= ::log] :string]
     ::s/handler
     (fn [_ctx _system message]
       (println "[{{name}}]" message)
       nil)}}})
