(ns scratchpad.state
  (:require [reagent.core :as r]))

;; The app's shared state. Because it's a Reagent atom, any component that
;; reads it with @db re-renders automatically when it changes - try
;; `(swap! db assoc :message "Hi!")` from the REPL.
;;
;; `defonce` (not `def`) means it keeps its value when the code hot-reloads.
(defonce db (r/atom {}))

;; Bumped after every hot reload (see scratchpad.main). Used by
;; scratchpad.ui/error-boundary to retry rendering once you've fixed an error.
(defonce reload-count (r/atom 0))
