(ns de.contentreich.ecm.page
  (:require
    [clojure.pprint :as pp]
    [com.stuartsierra.component :as c]
    [de.otto.tesla.stateful.handler :as handlers]
    [de.otto.status :as status]
    [de.otto.tesla.stateful.app-status :as app-status]
    [ring.middleware.params :as rmp]
    [compojure.core :as compojure]
    [compojure.route :as route]
    [hiccup.page :as page]
    [hiccup.form :as form]
    [clojure.tools.logging :as log]
    [ring.middleware.json :as json]
    [ring.middleware.multipart-params :as mp]

    [de.contentreich.ecm.google-vision :as vision]))

(defn head [title]
  [:head
   [:title title]
   [:meta {:http-equiv "Content-Type" :content "text/html" :charset "utf-8"}]
   [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
   (page/include-css "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap.min.css")])

(defn basic-template [title content]
  (page/html5 {:lang "en"}
              (head (str title))
              [:body
               [:div {:class "container"}
                [:div {:class "jumbotron"} content]]]))

(defn input-form []
  (form/form-to {:enctype "multipart/form-data"}
                [:post "/vision-form"]
                [:div {:class "input-group"}
                 (form/file-upload {:class "form-control"} "input")
                 [:span {:class "input-group-btn"} (form/submit-button {:class "btn btn-default"} "send")]]))

(defn usage-html []
  [:div
   [:h1 "Google Vision"]
   [:div (str "Upload an image file")]])

(defn total-calculations-html [x]
  [:div {:class "calculations"} (str x " calculations so far")])

(defn form-page [input result calculations-number]
  (basic-template "Google Vision Demo Page"
                  [:div
                   (usage-html)
                   (when-not (empty? result)
                     [:div {:class "resuĺt alert alert-info"} result])
                   (input-form)
                   (total-calculations-html calculations-number)]))

(defrecord Page []
  c/Lifecycle
  (start [self]
    (handlers/register-handler (:handler self)
                               (compojure/routes (compojure/GET "/vision-form" [_]
                                                   (let [x (vision/calculations (:calculator self))]
                                                     (form-page "" "" x)))
                                                 (mp/wrap-multipart-params
                                                   (compojure/POST "/vision-form" {params :params}
                                                     (if-let [input (get params "input")]
                                                       (let [is (java.io.FileInputStream. (:tempfile (get params "input")))
                                                             result (vision/calculate! (:calculator self) is)
                                                             calculations-number (vision/calculations (:calculator self))]
                                                         (form-page input (with-out-str (pp/pprint (:body result))) calculations-number)))))
                                                 (json/wrap-json-response
                                                   (compojure/POST "/vision" request
                                                     (vision/calculate! (:calculator self) (:body request))))

                                                 (route/not-found "Not Found")))
    (app-status/register-status-fun (:app-status self)
                                    (fn [] (status/status-detail :example-page :ok "page is always fine")))
    self)
  (stop [self]
    self))

(defn new-page [] (map->Page {}))