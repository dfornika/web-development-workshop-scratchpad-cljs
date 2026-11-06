(ns scratchpad.loaders
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [<!]]
            [cljs-http.client :as http]
            [scratchpad.state :refer [db]]))

(defn load-json-rows
  "Fetch `url` and, if the response is a JSON array of objects, store it in
  the db at `path`. Anything else is logged and ignored.

  Checking the status alone isn't enough: a dev server (or a misconfigured
  web server) may answer a request for a missing file with index.html and
  a 200 status."
  [url path]
  (go
    (let [{:keys [status body]} (<! (http/get url))]
      (if (and (= 200 status)
               (sequential? body)
               (every? map? body))
        (swap! db assoc-in path body)
        (js/console.warn "Ignoring unexpected response for" url
                         (clj->js {:status status :body-type (str (type body))}))))))

(defn load-json-map
  "Fetch `url` and, if the response is a JSON object, store it in the db
  at `path`. Anything else is logged and ignored."
  [url path]
  (go
    (let [{:keys [status body]} (<! (http/get url))]
      (if (and (= 200 status) (map? body))
        (swap! db assoc-in path body)
        (js/console.warn "Ignoring unexpected response for" url
                         (clj->js {:status status :body-type (str (type body))}))))))

(defn load-config
  "Load the config from config.json."
  []
  (load-json-map "config.json" [:config]))