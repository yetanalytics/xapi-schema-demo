(ns ^:figwheel-always xapi-schema-demo.core
    (:require
     [reagent.core :as r :refer [atom]]
     [xapi-schema.core :as xs]
     [reagent-forms.core :refer [bind-fields]]
     [json-html.core :as js-html]
     [cljs.core.match :refer-macros [match]]))

(enable-console-print!)


(def simple-statement-str
  "{
    \"id\":\"fd41c918-b88b-4b20-a0a5-a4c32391aaa0\",
    \"actor\":{
        \"objectType\": \"Agent\",
        \"name\":\"Project Tin Can API\",
        \"mbox\":\"mailto:user@example.com\"
        },
    \"verb\":{
        \"id\":\"http://example.com/xapi/verbs#sent-a-statement\",
        \"display\":{
            \"en-US\":\"sent\"
        }
    },
    \"object\":{
        \"id\":\"http://example.com/xapi/activity/simplestatement\",
        \"definition\":{
            \"name\":{
                \"en-US\":\"simple statement\"
            },
            \"description\":{
                \"en-US\":\"A simple Experience API statement. Note that the LRS
                does not need to have any prior information about the Actor (learner), the
                verb, or the Activity/object.\"
            }
        }
    }
}")

(def simple-statement-edn
  {"id" "fd41c918-b88b-4b20-a0a5-a4c32391aaa0",
   "actor" {"objectType" "Agent"
            "name" "Project Tin Can API"
            "mbox" "mailto:user@example.com"}
   "verb" {"id" "http://example.com/xapi/verbs#sent-a-statement",
           "display" {"en-US" "sent"}}
   "object" {"id" "http://example.com/xapi/activity/simplestatement",
             "definition"
             {"name" {"en-US" "simple statement"}
              "description" {"en-US" "A simple Experience API statement. Note that the LRS does not need to have any prior information about the Actor (learner), the verb, or the Activity/object."}}}})

(defonce app-state (atom {:statement-input simple-statement-str
                          :statement simple-statement-edn}))

(defonce form-template
  [:div.form-group
   [:textarea.form-control {:field :textarea :id :statement-input :rows "25"}]])

(defn process-input
  [kp val doc]
  (when (= kp [:statement-input])
    (let [[statement parse-err]
          (try
            [(js->clj (.parse js/JSON (clojure.string/replace val #"\n" ""))) nil]
            (catch js/SyntaxError e
              [nil e]))

          validation-err (when-let [e (xs/statement-checker statement)]
                                 (xs/errors->data e))
          err (or parse-err
                  validation-err)]
      (cond-> doc
        err (assoc :error err)
        (and statement (nil? err))
        (assoc :statement statement
               :error nil)))))


(defn demo []
  (let [{:keys [statement error]} @app-state]
    [:div.container-fluid
     [:div.jumbotron
      [:h2 "xAPI Statement Validator"]
      [:p "Enter JSON statements in the text area. The statement will be validated and any errors will be displayed."]]
     [:div.row
      [:div.col-md-6
       [:div.panel
        [:div.panel-body
         [bind-fields
          form-template
          app-state
          process-input]]]]
      [:div.col-md-6
       (if (and error (instance? js/SyntaxError error))
         [:div.panel
          [:div.panel-heading
           [:h4.panel-title "Syntax Error:"]]
          [:div.panel-body.bg-danger
          (str error)]]
         (if error
           [:div.panel
            [:div.panel-heading
             [:h4.panel-title "Validation Error:"]]
            [:div.panel-body.bg-danger
             (js-html/edn->hiccup error)]]
           [:div.panel
            [:div.panel-heading
             [:h4.panel-title "Valid Statement:"]]
            [:div.panel-body
            (js-html/edn->hiccup statement)]])
         )]]]))

(r/render [demo] (.getElementById js/document "app"))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)
