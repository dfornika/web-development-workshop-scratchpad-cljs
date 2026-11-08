(ns scratchpad.views.fetching
  "The Fetching Data page: loading JSON files from a data/ directory next
  to index.html, with cljs-http and core.async, and putting what arrives
  into the app's state.

  Every example can be edited and run on the page. They're run by SCI, in
  the namespace set up by `setup-source` (shown on the page), against the
  real files in resources/public/data/."
  (:require [reagent.core :as r]
            [scratchpad.components :as c]
            [scratchpad.ui :as ui]
            [scratchpad.views.reference :as reference :refer [mono snippet table]]
            [scratchpad.web.sci :as sci]
            [scratchpad.web.sci-async :as sci-async]))

;; ---------------------------------------------------------------------------
;; Where the examples run
;; ---------------------------------------------------------------------------

(def ^:private setup-source
  (str "(ns my-site.data\n"
       "  (:require [cljs.core.async :refer [go <!]]\n"
       "            [cljs-http.client :as http]\n"
       "            [reagent.core :as r]\n"
       "            [scratchpad.components :as c]))\n"
       "\n"
       ";; The app's state: everything the page shows comes from here.\n"
       ";; (In this project, it's scratchpad.state/db.)\n"
       "(defonce db (r/atom {}))"))

;; Made the first time an example is drawn, by running setup-source - the
;; same text the page shows.
(defonce ^:private examples-ns
  (delay (sci/create-ns! setup-source)))

(defn- example
  "An example you can edit and run. Examples that fetch something only run
  when asked (`:run-at-start? false`)."
  ([code] (example code {}))
  ([code opts] [reference/example examples-ns code opts]))

(defn- fetches
  "An example that fetches data: run only when asked."
  [code]
  [example code {:run-at-start? false}])

(defn- examples-var
  "The current value of a var made by the examples, e.g. 'db."
  [sym]
  @examples-ns   ; make sure the namespace exists
  (sci/resolve-var (symbol "my-site.data" (name sym))))

;; Requests here are slowed down (while this page is open), so you can see
;; the channel arrive first and the data after. The choice is remembered
;; while the site is open.
(def ^:private delay-ms 1000)
(defonce ^:private slow? (r/atom true))

(defn- set-delay! []
  (reset! sci-async/request-delay-ms (if @slow? delay-ms 0)))

(defn- slow-toggle
  "The checkbox that turns the pretend delay on and off."
  []
  [:label {:style {:display "flex" :align-items "center" :gap "8px"
                   :margin "12px 0" :padding "8px 12px"
                   :border "1px solid #d4a72c" :border-radius "6px"
                   :background-color "#fff8c5" :font-size "14px" :cursor "pointer"}}
   [:input {:type "checkbox"
            :checked @slow?
            :on-change (fn [e]
                         (reset! slow? (.. e -target -checked))
                         (set-delay!))}]
   [:span [:strong "Slow down the requests on this page"]
    " - each one waits an extra second, on purpose, so you can see what "
    "happens while it's on its way. (These files are on the same server as "
    "the page, so they'd otherwise arrive almost instantly.)"]])

;; ---------------------------------------------------------------------------
;; Live views of what the examples make
;; ---------------------------------------------------------------------------

(defn- db-panel
  "Shows the examples' db, live."
  []
  (let [db (examples-var 'db)]
    [:div {:style {:margin "12px 0"
                   :border "1px solid #54aeff"
                   :border-radius "6px"
                   :background-color "#ddf4ff"}}
     [:div {:style {:display "flex" :align-items "center" :gap "8px"
                    :padding "6px 10px" :font-size "13px" :color "#004a87"}}
      [:strong "db"] [:span "- what's in the examples' app state right now"]
      [:span {:style {:margin-left "auto"}}
       [c/button {:variant :secondary
                  :style {:padding "1px 8px" :font-size "12px"}
                  :on-click #(reset! db {})}
        "Clear db"]]]
     [:pre {:style {:margin 0 :padding "8px 10px" :max-height "260px" :overflow "auto"
                    :background-color "white" :border-radius "0 0 6px 6px"
                    :font-size "12px" :white-space "pre-wrap"}}
      (let [value @db]
        (if (empty? value)
          "{} - empty. Run an example that loads something!"
          (binding [*print-length* 20]
            (reference/pretty value))))]]))

(defn- preview
  "Draws the component `sym` made by the examples, live."
  [sym]
  (let [_ @reference/run-count   ; redraw after examples run
        component (examples-var sym)]
    [:div {:style {:margin "12px 0"
                   :padding "12px"
                   :border "1px dashed #d0d7de"
                   :border-radius "6px"
                   :background-color "white"}}
     [:div {:style {:font-size "12px" :color "#57606a" :margin-bottom "8px"}}
      "Preview of " [:code (str "[" sym "]")]]
     (if (fn? component)
       ^{:key @reference/run-count} [ui/error-boundary [component]]
       [:span {:style {:color "#57606a"}} "Run the example above to define it."])]))

;; ---------------------------------------------------------------------------
;; Sections
;; ---------------------------------------------------------------------------

(defn- box [title & lines]
  [c/card {:style {:flex "1 1 160px" :text-align "center"}}
   [:div {:style {:font-weight "bold" :margin-bottom "6px"}} title]
   (into [:div {:style {:font-size "13px" :color "#57606a" :line-height "1.4"}}]
         (interpose [:br] lines))])

(defn- arrow [label]
  [:div {:style {:display "flex" :flex-direction "column" :align-items "center"
                 :justify-content "center" :font-size "12px" :color "#57606a"
                 :min-width "70px"}}
   [:div {:style {:font-size "22px" :color "#004a87"}} "→"]
   label])

(defn- file-link [path]
  [c/link {:href path :target "_blank"} [:code path]])

(defn- file-warning []
  (when (= "file:" (.. js/window -location -protocol))
    [c/callout {:kind :warning :style {:margin-bottom "16px"}}
     [:strong "This copy of the site is a single file, so it can't fetch anything "]
     "(browsers only fetch over http). The examples that fetch data won't work "
     "here - open the published site, or the app in Codespaces, to try them."]))

(defn- where-from []
  [:<>
   [file-warning]
   [c/paragraph
    "Sites like the ones in this workshop usually show data that something "
    "else produces - a pipeline, a scheduled job, a script someone runs. A "
    "simple, sturdy way to connect the two: that process writes JSON files "
    "into a " [:code "data/"] " directory next to the site's "
    [:code "index.html"] ", and the page fetches the files it needs, when "
    "it needs them."]
   [c/row {:gap "8px" :align "stretch"}
    [box "On the server" "a pipeline or job" "writes JSON files"]
    [arrow "into"]
    [box "data/" "runs.json" "runs/RUN_001.json" "summary.json"]
    [arrow "fetch"]
    [box "The page" "fetches what it needs" "and puts it in the db"]
    [arrow "redraws"]
    [box "Components" "show what's" "in the db"]]
   [c/paragraph {:style {:margin-top "16px"}}
    "Each URL gives you one piece of data, as JSON - like a very simple "
    "REST API, made of files. This site has a few to practise with (open "
    "them to see what's inside):"]
   [table ["URL" "What's in it"]
    [[[file-link "data/summary.json"] "A map: when the data was last updated, and some totals"]
     [[file-link "data/runs.json"] "A list of sequencing runs"]
     [[file-link "data/runs/RUN_001.json"]
      [:span "The samples in one run (also RUN_002, RUN_003 and RUN_004) - "
       "fetched only when someone wants to see that run"]]]]
   [c/paragraph {:style {:margin-top "12px"}}
    "The URLs don't start with a " [:code "/"] ": they're relative to the "
    "page, so they work wherever the site lives - at the top of a server, or "
    "in a folder like this site on GitHub Pages."]])

(defn- with-cljs-http []
  [:<>
   [c/paragraph
    "The examples on this page run in this namespace. Its " [:code "db"]
    " is the app state: everything we fetch will end up there."]
   [snippet setup-source]
   [c/paragraph
    [:code "http/get"] " (from the cljs-http library) asks for a URL. It "
    [:em "doesn't"] " wait for the answer - it gives back a " [:em "channel"]
    " straight away, and the response arrives on it later. Run this one "
    "and watch: first the channel, then the response."]
   [slow-toggle]
   [fetches "(http/get \"data/summary.json\")"]
   [c/paragraph
    "The response is a map. The useful parts are " [:code ":status"]
    " (200 means OK) and " [:code ":body"]
    ". cljs-http has already turned the JSON in the body into ClojureScript "
    "data, with keywords for keys - so " [:code "\"run_count\""] " became "
    [:code ":run_count"] "."]])

(defn- go-blocks []
  [:<>
   [c/paragraph
    "To " [:em "use"] " the response, we need to wait for it. That's what "
    [:code "go"] " and " [:code "<!"] " (from core.async) are for. Inside a "
    [:code "go"] " block, " [:code "<!"] " waits for a channel's value - "
    "without freezing the page while it waits:"]
   [fetches (str "(go\n"
                 "  (let [response (<! (http/get \"data/summary.json\"))]\n"
                 "    (:body response)))")]
   [c/paragraph
    "Read it top to bottom, as if it ran in order: ask for the file, wait "
    "for the response, take its body. " [:code "go"] " itself gives back a "
    "channel too, which gets whatever the block ends with."]
   [c/callout {:kind :info}
    [:strong "Only inside go. "]
    [:code "<!"] " only works inside a " [:code "go"] " block. And code "
    [:em "after"] " the go block doesn't wait for it - which is why the "
    "next step, putting the data in the db, happens inside it."]
   [c/paragraph {:style {:margin-top "12px" :font-size "13px" :color "#57606a"}}
    "(Here in the browser, these examples run in an interpreter with a simple "
    "stand-in for " [:code "go"] " and " [:code "<!"] ", made with JavaScript "
    "promises. It works the same way for everything on this page. In "
    "Codespaces, you get the real core.async.)"]])

(defn- into-the-db []
  [:<>
   [c/paragraph
    "Here's the whole pattern: fetch, wait, and " [:code "swap!"]
    " the data into the db. Anything that shows the db redraws by itself."]
   [example (str "(defn load-runs! []\n"
                 "  (go\n"
                 "    (let [{:keys [status body]} (<! (http/get \"data/runs.json\"))]\n"
                 "      (if (= 200 status)\n"
                 "        (swap! db assoc :runs body)\n"
                 "        (swap! db assoc :error (str \"Couldn't load runs.json (\" status \")\"))))))")]
   [c/paragraph "Defining it doesn't fetch anything. Calling it does:"]
   [fetches "(load-runs!)"]
   [db-panel]
   [c/paragraph
    "A component that shows the runs just reads the db. While they haven't "
    "arrived, " [:code "(:runs @db)"] " is " [:code "nil"] ", so it shows a "
    "message instead:"]
   [example (str "(defn runs-list []\n"
                 "  (let [runs (:runs @db)]\n"
                 "    (if (nil? runs)\n"
                 "      [:p \"No runs yet...\"]\n"
                 "      [:ul\n"
                 "       (for [{:keys [run_id run_date sample_count]} runs]\n"
                 "         ^{:key run_id}\n"
                 "         [:li run_id \" (\" run_date \"): \" sample_count \" samples\"])])))")]
   [preview 'runs-list]
   [c/paragraph
    "Try it: press " [:strong "Clear db"] ", then run " [:code "(load-runs!)"]
    " again, and watch the list fill in."]])

(defn- on-demand []
  [:<>
   [c/paragraph
    "There's no need to fetch everything at the start. Here, a run's "
    "samples are only fetched when someone picks that run - and only the "
    "first time, since after that they're already in the db."]
   [example (str "(defn load-run-samples! [run-id]\n"
                 "  ;; already loaded? then there's nothing to do\n"
                 "  (when-not (get-in @db [:samples run-id])\n"
                 "    (go\n"
                 "      (let [url (str \"data/runs/\" run-id \".json\")\n"
                 "            {:keys [status body]} (<! (http/get url))]\n"
                 "        (when (= 200 status)\n"
                 "          (swap! db assoc-in [:samples run-id] body))))))\n"
                 "\n"
                 "(defn select-run! [run-id]\n"
                 "  (swap! db assoc :selected-run run-id)\n"
                 "  (load-run-samples! run-id))")]
   [example (str "(defn run-browser []\n"
                 "  (let [{:keys [runs selected-run samples]} @db]\n"
                 "    (if (nil? runs)\n"
                 "      [:p \"No runs yet - run (load-runs!) above.\"]\n"
                 "      [:div\n"
                 "       [c/row {:gap \"6px\"}\n"
                 "        (for [{:keys [run_id]} runs]\n"
                 "          ^{:key run_id}\n"
                 "          [c/button {:variant (if (= run_id selected-run) :primary :secondary)\n"
                 "                     :on-click #(select-run! run_id)}\n"
                 "           run_id])]\n"
                 "       (when selected-run\n"
                 "         (if-let [run-samples (get samples selected-run)]\n"
                 "           [:p selected-run \": \" (count run-samples) \" samples, \"\n"
                 "            (count (filter #(= \"PASS\" (:qc_status %)) run-samples))\n"
                 "            \" passed QC\"]\n"
                 "           [:p \"Loading \" selected-run \"...\"]))])))")]
   [preview 'run-browser]
   [db-panel]
   [c/paragraph
    "Click through the runs and watch " [:code ":samples"] " in the db grow, "
    "one run at a time. (Clicking a run again doesn't fetch it again.)"]])

(defn- going-wrong []
  [:<>
   [c/paragraph
    "Files go missing, servers have bad days. Always check the "
    [:code ":status"] " before using the body:"]
   [fetches (str "(go\n"
                 "  (let [{:keys [status success body]} (<! (http/get \"data/no-such-file.json\"))]\n"
                 "    {:status status\n"
                 "     :success success\n"
                 "     :got-data? (coll? body)}))")]
   [c/paragraph
    "On a normal web server (like GitHub Pages) that's a 404. But some "
    "servers - including the development server in Codespaces - answer a "
    "request for a missing file with the site's " [:code "index.html"]
    " and a 200! So a careful loader also checks the body is the kind of "
    "data it expected:"]
   [snippet (str "(defn load-runs! []\n"
                 "  (go\n"
                 "    (let [{:keys [status body]} (<! (http/get \"data/runs.json\"))]\n"
                 "      (if (and (= 200 status) (sequential? body) (every? map? body))\n"
                 "        (swap! db assoc :runs body :error nil)\n"
                 "        (swap! db assoc :error \"Couldn't load the runs.\")))))")]
   [c/paragraph
    "Then a component can show " [:code "(:error @db)"]
    " if there is one. This project has loaders like that ready to use, in "
    [:code "src/main/scratchpad/loaders.cljs"] ": " [:code "load-json-rows"]
    " (for a list of maps) and " [:code "load-json-map"] "."]])

(defn- with-fetch []
  [:<>
   [c/paragraph
    "Browsers also have " [:code "js/fetch"] " built in, and recent "
    "ClojureScript (1.12) can use it with " [:code "async"] " and "
    [:code "await"] ", much like JavaScript. Mark a function "
    [:code "^:async"] ", and " [:code "await"] " waits for a promise inside it:"]
   [example (str "(defn ^:async load-summary! []\n"
                 "  (let [response (await (js/fetch \"data/summary.json\"))\n"
                 "        data (await (.json response))]\n"
                 "    (swap! db assoc :summary (js->clj data :keywordize-keys true))))")]
   [fetches "(load-summary!)"]
   [db-panel]
   [c/paragraph
    "It's the same shape as the go block, with a few differences:"]
   [table ["" "cljs-http + core.async" "js/fetch + async/await"]
    [["Gives back" "a channel" "a promise"]
     ["Wait with" [:span [:code "<!"] ", inside " [:code "go"]] [:span [:code "await"] ", inside an " [:code "^:async"] " function"]]
     ["The body" "already ClojureScript data"
      [:span "needs " [:code "(.json response)"] " and " [:code "js->clj"]]]
     ["Status" [:code "(:status response)"] [:code "(.-status response)"]]
     ["Needs" "two libraries (in this project already)" "nothing - it's built in"]]]
   [c/paragraph {:style {:margin-top "12px"}}
    "Either works well. The workshop's sites use cljs-http and core.async, "
    "and channels can do much more than wait for one response. But "
    [:code "fetch"] " is everywhere in JavaScript examples, so it's good to "
    "recognise. (You'll also see it written with " [:code ".then"]
    ": " [:code "(-> (js/fetch url) (.then #(.json %)) (.then ...))"] ".)"]])

(defn- own-project []
  [:<>
   [c/paragraph
    "In Codespaces, put your JSON files in " [:code "resources/public/data/"]
    " - that folder is served as " [:code "data/"] " next to the app. Then, "
    "in one of your files:"]
   [snippet (str "(ns scratchpad.playground\n"
                 "  (:require [cljs.core.async :refer [go <!]]\n"
                 "            [cljs-http.client :as http]\n"
                 "            [scratchpad.state :refer [db]]\n"
                 "            ...))\n"
                 "\n"
                 "(defn load-runs! []\n"
                 "  (go\n"
                 "    (let [{:keys [status body]} (<! (http/get \"data/runs.json\"))]\n"
                 "      (when (= 200 status)\n"
                 "        (swap! db assoc :runs body)))))\n"
                 "\n"
                 "(comment\n"
                 "  (load-runs!)\n"
                 "  (:runs @db))")]
   [c/paragraph
    "When to call it: once when the page starts, for things every page "
    "needs; or when a component first needs the data, like "
    [:code "select-run!"] " above. Calling it from a " [:code "(comment ...)"]
    " block is handy while you build."]])

(defn- gotchas []
  [table ["Watch out for" "What happens" "What to do"]
   [[[:span "A URL starting with " [:code "/"] ", like " [:code "\"/data/runs.json\""]]
     "Works on your computer, but not when the site is in a folder (like on GitHub Pages)"
     [:span "Use relative URLs: " [:code "\"data/runs.json\""]]]
    [[:span "Using " [:code "<!"] " outside " [:code "go"]]
     "An error"
     [:span "Put the code that needs the response inside the " [:code "go"] " block"]]
    [[:span "Expecting " [:code "(load-runs!)"] " to give you the data"]
     "It gives a channel, straight away - the data arrives later"
     [:span "Put the data in the db inside the " [:code "go"] " block, and read it from the db"]]
    [[:span "Opening the site as a file (" [:code "file://"] "), like the single-file version"]
     "Fetching fails: browsers only fetch over http(s)"
     "Use a web server - in Codespaces, the dev server on port 8000"]
    ["The file changed on the server, but the page shows the old data"
     "The browser reused a copy it fetched before"
     [:span "Reload the page. Or add something that changes to the URL: "
      [:code "(str \"data/runs.json?t=\" (js/Date.now))"]]]
    ["Fetching from a different website"
     "Often blocked by the browser (CORS), unless that site allows it"
     "Keep the data on the same server as the site, or ask whoever runs the other site"]]])

(defn- page-content []
  [reference/page
   {:id "fetching"
    :title "Fetching Data"
    :intro ["How a page gets its data: fetching JSON files from the server with "
            "cljs-http and core.async, and putting what arrives into the app's "
            "state. " [:strong "Every example is live:"] " the ones that fetch "
            "data run when you press " [:strong "Ctrl+Enter"] " or "
            [:strong "▶ Run"] ", against real files on this site. (They're "
            "slowed down a little on purpose, so you can watch them arrive.)"]
    :sections [{:title "Where the data comes from" :content where-from}
               {:title "Asking for a file: cljs-http" :content with-cljs-http}
               {:title "Waiting for it: go and <!" :content go-blocks}
               {:title "Putting it in the db" :content into-the-db}
               {:title "Fetching on demand" :content on-demand}
               {:title "When things go wrong" :content going-wrong}
               {:title "The other way: fetch and await" :content with-fetch}
               {:title "In your own project" :content own-project}
               {:title "Gotchas" :content gotchas}]}])

(defn page []
  (r/with-let [_ (set-delay!)]
    [page-content]
    ;; leaving the page: requests elsewhere aren't slowed down
    (finally (reset! sci-async/request-delay-ms 0))))
