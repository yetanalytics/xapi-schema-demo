(ns ^:figwheel-always xapi-schema-demo.core
    (:require
     [reagent.core :as r :refer [atom]]
     [xapi-schema.core :as xs]
     [cljs.pprint :refer [pprint]]
     [clojure.string :as cs]
     [xapi-schema.spec :as xapispec]
     [clojure.spec.alpha :as s :include-macros true]
     [clojure.spec.gen.alpha :as sgen :include-macros true]
     clojure.test.check.generators))

(enable-console-print!)

(defn gen-statement-json-str []
  (.stringify js/JSON
              (clj->js (sgen/generate (s/gen ::xapispec/statement)))
              nil
              4))

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

(defn parse-json-str [json-str]
  (js->clj (.parse js/JSON (cs/replace json-str #"\n\s*" " "))))

(def simple-statement-edn
  (parse-json-str simple-statement-str))

(defonce app-state (atom {:statement-input simple-statement-str
                          :statement simple-statement-edn}))

(defonce input-cursor
  (r/cursor app-state [:statement-input]))

(defonce form-template
  [:div.form-group
   [:textarea.form-control {:field :textarea :id :statement-input :rows "25"}]])

(defn process-input
  [doc val]
  (let [[statement parse-err]
        (try
          [(parse-json-str val) nil]
          (catch js/SyntaxError e
            [nil e]))

        validation-err (when-let [e (xs/statement-checker statement)]
                         e)
        err (or parse-err
                validation-err)]
    (cond-> (assoc doc :statement-input val)
      err (assoc :error err)
      (and statement (nil? err))
      (assoc :statement statement
             :error nil))))



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
         [:button.btn.btn-primary {:type "button" :on-click #(swap! app-state process-input (gen-statement-json-str))} "Generate Random Statement"]
         [:textarea.statement-input
          {;; :rows "30"
           :value @input-cursor
           :on-change #(swap! app-state process-input (-> % .-target .-value))}]]]]
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
             [:pre.error
              (with-out-str
                (pprint error))]]]
           [:div.panel
            [:div.panel-heading
             [:h4.panel-title "Valid Statement:"]]
            [:div.panel-body
             [:pre.statement
              (with-out-str
                (pprint statement))]]])
         )]]]))

(r/render [demo] (.getElementById js/document "app"))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)
