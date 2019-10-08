(ns reframe-talk.core
  (:require
    [reagent.core :as reagent]
    [re-frame.core :as rf]
    [breaking-point.core :as bp]
    [reframe-talk.app :as app]
    ["@blueprintjs/core" :refer [FocusStyleManager]]))

(def debug?
  ^boolean goog.DEBUG)

(defn dev-setup []
  (when debug?
    (enable-console-print!)
    (println "dev mode")))

(defn ^:dev/after-load start
  []
  (rf/clear-subscription-cache!)
  (dev-setup)
  (.onlyShowFocusOnTabs FocusStyleManager)
  (reagent/render
    [app/root-view]
    (.getElementById js/document "app")))

(defn ^:export init
  []
  (rf/dispatch-sync [::app/initialize-db])
  (rf/dispatch-sync [::bp/set-breakpoints
                     {:breakpoints [:xs
                                    576
                                    :sm
                                    768
                                    :md
                                    992
                                    :lg
                                    1200
                                    :xl]
                      :debounce-ms 50}])
  (start))

(defn ^:dev/before-load stop
  []
  (println "stop"))
