(ns scratchpad.web.sci-async
  "core.async and cljs-http for code run by SCI (the in-browser version,
  and the live examples on the reference pages).

  The functions - chan, put!, take!, http/get... - are the real ones. But
  `go` and `<!` are macros that core.async implements by rewriting your
  code when it's compiled, which SCI can't do. So here they're a small
  stand-in built on JavaScript promises, using SCI's async/await support:

    (go body...)  runs body in an async function, and returns a channel
                  that gets body's result (or just closes, if it's nil)
    (<! ch)       inside go: waits for a value from ch

  That covers the usual pattern - (go (let [r (<! (http/get url))] ...)) -
  but not everything real go blocks can do (like alts!). In Codespaces you
  get the real core.async."
  (:require [cljs-http.client :as http]
            [cljs.core.async :as async]))

(defn chan->promise
  "A promise of the next value from channel `ch`."
  [ch]
  (js/Promise. (fn [resolve] (async/take! ch resolve))))

(defn promise->chan
  "A channel that gets what `promise` resolves to, then closes."
  [^js promise]
  (let [ch (async/chan 1)]
    (.then promise
           (fn [value]
             (if (some? value)
               (async/put! ch value #(async/close! ch))
               (async/close! ch)))
           (fn [error]
             ;; like a real go block: report the error, and close the channel
             (js/console.error "Error in go block:" error)
             (async/close! ch)))
    ch))

;; SCI knows a function is a macro by :sci/macro in the function's own
;; metadata (not the var's), so these are made with with-meta.

(def go
  (with-meta
    (fn [_&form _&env & body]
      (list 'cljs.core.async/promise->chan
            (list (with-meta (list* 'fn [] body) {:async true}))))
    {:sci/macro true}))

(def <!
  (with-meta
    (fn [_&form _&env ch]
      (list 'await (list 'cljs.core.async/chan->promise ch)))
    {:sci/macro true}))

(def namespaces
  "For SCI's :namespaces option."
  {'cljs.core.async {'go go
                     '<! <!
                     'chan async/chan
                     'put! async/put!
                     'take! async/take!
                     'close! async/close!
                     'timeout async/timeout
                     ;; used by go and <!
                     'promise->chan promise->chan
                     'chan->promise chan->promise}
   'cljs-http.client {'get http/get
                      'post http/post
                      'put http/put
                      'delete http/delete
                      'request http/request}})
