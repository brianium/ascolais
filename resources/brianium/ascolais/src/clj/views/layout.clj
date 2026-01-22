(ns {{top/ns}}.views.layout
  "Base layout and page shells."
  (:require [dev.onionpancakes.chassis.core :as c]
            [{{top/ns}}.views.components]))

(defn base-layout
  "Base HTML layout with Datastar."
  [{:keys [title]} & body]
  [c/doctype-html5
   [:html {:lang "en"}
    [:head
     [:meta {:charset "utf-8"}]
     [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
     [:title title]
     [:script {:type "module"
               :src "https://cdn.jsdelivr.net/npm/@starfederation/datastar@1.0.0-rc.2/dist/datastar.min.js"}]
     [:style (c/raw "
       body { font-family: system-ui, sans-serif; max-width: 800px; margin: 0 auto; padding: 2rem; }
       .greeting { padding: 1rem; background: #f0f0f0; border-radius: 8px; margin-top: 1rem; }
       form { display: flex; gap: 0.5rem; margin-top: 1rem; }
       input { padding: 0.5rem; border: 1px solid #ccc; border-radius: 4px; }
       button { padding: 0.5rem 1rem; background: #0066cc; color: white; border: none; border-radius: 4px; cursor: pointer; }
       button:hover { background: #0052a3; }
       nav { margin-bottom: 2rem; }
       nav a { margin-right: 1rem; color: #0066cc; }
     ")]]
    [:body
     [:nav
      [:a {:href "/"} "Home"]
      [:a {:href "/about"} "About"]
      [:a {:href "/sandbox"} "Sandbox"]]
     body]]])

(defn home-page
  "Home page view."
  []
  (base-layout {:title "{{name}}"}
    [:h1 "Welcome to {{name}}"]
    [:p "A Clojure web application powered by the sandestin effect ecosystem."]

    [:div {:data-signals "{name: ''}"}
     [:form {:data-on:submit__prevent "@post('/api/greet')"}
      [:input {:type "text"
               :placeholder "Enter your name"
               :data-bind:name true}]
      [:button {:type "submit"} "Greet"]]

     [:div#greeting]]))

(defn about-page
  "About page view."
  []
  (base-layout {:title "About - {{name}}"}
    [:h1 "About"]
    [:p "This application demonstrates the sandestin effect ecosystem:"]
    [:ul
     [:li [:strong "sandestin"] " - Effect dispatch with schema-driven discoverability"]
     [:li [:strong "twk"] " - Datastar SSE integration"]
     [:li [:strong "sfere"] " - Connection management and broadcasting"]
     [:li [:strong "kaiin"] " - Declarative HTTP routing from registry metadata"]
     [:li [:strong "manse"] " - Database effects with next.jdbc"]
     [:li [:strong "tsain"] " - Component development sandbox"]]
    [:p [:a {:href "/"} "Back to home"]]))
