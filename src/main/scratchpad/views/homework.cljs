(ns scratchpad.views.homework
  "Building blocks for the session Homework pages.

  Each homework page shows the live results of functions in its session's
  files (src/main/scratchpad/sessions/). It finds those functions through
  a `lookup` function - (lookup :s1 'count-bases) - so the same page works
  in Codespaces (compiled code) and in the in-browser version (code typed
  into the page, run by SCI).

  The functions here take a context map:
    :lookup       finds a homework function or value: (lookup file-key symbol)
    :files        file-key -> {:name \"session_1.cljs\" :path \"src/main/...\" :ns \"...\"}
    :in-browser?  true when the files are being edited on the page"
  (:require [scratchpad.components :as c]
            [scratchpad.ui :as ui]))

(defn- missing [{:keys [files]} file sym]
  (str "Can't find " sym " in " (get-in files [file :name])
       " - has it been renamed or deleted?"))

(defn component
  "Draws the homework component `sym` (from `file`) with `args`."
  [{:keys [lookup] :as ctx} file sym & args]
  (let [f (lookup file sym)]
    (if (fn? f)
      (into [f] args)
      [c/callout {:kind :warning} (missing ctx file sym)])))

(defn call
  "Shows a call like (count-bases \"ATG\") together with its result."
  [{:keys [lookup] :as ctx} file sym & args]
  [ui/call-result
   (pr-str (cons sym args))
   (fn []
     (let [f (lookup file sym)]
       (if (fn? f)
         (apply f args)
         (throw (ex-info (missing ctx file sym) {})))))])

(defn edit
  "The \"Edit ...\" line of an exercise: which function(s), in which file."
  [{:keys [files in-browser?]} file what]
  (if in-browser?
    (str what " in " (get-in files [file :name]) " (on the left)")
    (str what " in " (get-in files [file :path]))))

(defn intro
  "How the page works. `main-file` is the file key of the main homework file."
  [{:keys [files in-browser?]} main-file]
  (if in-browser?
    [c/callout
     [:strong "How this works: "]
     "each panel shows the live result of a function you write in the files on "
     "the left. Work through the TODOs, try things out in the "
     [:code "(comment ...)"] " blocks with Alt+Enter, and press Ctrl+S to load "
     "the file and update these panels."]
    [c/callout
     [:strong "How this page works: "]
     "each panel below shows the live result of a function you write. Open "
     [:code (get-in files [main-file :path])] ", load it into the REPL (Ctrl+Alt+C Enter), "
     "and work through the TODOs. Try things out in the " [:code "(comment ...)"]
     " blocks with Alt+Enter, and save the file to see this page update."]))
