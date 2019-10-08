(ns reframe-talk.app
  (:require
    [reagent.core :as reagent]
    [re-frame.core :as rf]
    [breaking-point.core :as bp]
    [day8.re-frame.http-fx]
    [re-frame.core :as rf]
    [com.degel.re-frame.storage]
    [day8.re-frame.tracing :refer-macros [fn-traced defn-traced]]))

(rf/reg-event-db
  ::initialize-db
  (fn-traced [_ _]
    {}))

(defn root-view
  []
  [:h1 "Hello World"])

