(ns scratchpad.macros
  "Macros used by the site's built-in pages. You don't need to edit these."
  (:require [clojure.pprint :as pp]
            [clojure.string :as str]))

(defn- strip-map-commas
  "pprint separates map entries with commas; remove those (but not commas
  inside strings) so snippets look like hand-written code."
  [s]
  (loop [[ch & more] s, in-string? false, escaped? false, out (StringBuilder.)]
    (cond
      (nil? ch) (str out)
      escaped? (recur more in-string? false (.append out ch))
      (and in-string? (= ch \\)) (recur more true true (.append out ch))
      (= ch \") (recur more (not in-string?) false (.append out ch))
      (and (= ch \,) (not in-string?)) (recur more false false out)
      :else (recur more in-string? false (.append out ch)))))

(defn- format-code [form]
  (str/trimr
   (strip-map-commas
   (binding [pp/*print-right-margin* 56]
     (with-out-str
       (pp/with-pprint-dispatch pp/code-dispatch
         (pp/pprint form)))))))

(defmacro example
  "Captures a hiccup `form` both as data to render and as formatted source
  code to display, so the two can never drift apart."
  [title description form]
  `{:title ~title
    :description ~description
    :code ~(format-code form)
    :view ~form})

(defmacro show-call
  "Renders a call like `(count-bases \"ATG\")` together with its result.
  The call happens inside the rendered component, so errors are caught by
  the surrounding error boundary."
  [form]
  (let [display-form (if (and (seq? form) (symbol? (first form)))
                       ;; show `(count-bases ...)` rather than `(s1/count-bases ...)`
                       (cons (symbol (name (first form))) (rest form))
                       form)]
    `[scratchpad.ui/call-result ~(format-code display-form) (fn [] ~form)]))
