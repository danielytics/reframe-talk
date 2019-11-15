;; Handlers

(rf/reg-event-fx
  ::initialize-db
  (fn-traced [_ _]
    {:db {}
     :dispatch [::set-page :stories]}))


(rf/reg-event-db
  ::received-stories
  (fn-traced [db [_ stories]]
    (assoc db :stories stories)))


(rf/reg-event-fx
  ::load-stories
  (fn-traced [_ _]
    {:http-xhrio {:method :get
                  :uri "http://localhost:3000/"
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success [::received-stories] 
                  :on-failure [::xhr-failure]}}))


(rf/reg-event-fx
  ::submit-story
  (fn-traced [_ [_ submission]]
    {:http-xhrio {:method :post
                  :uri "http://localhost:3000/stories"
                  :params submission
                  :format (ajax/json-request-format)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success [::set-page :stories] 
                  :on-failure [::xhr-failure]}}))


(rf/reg-event-fx
  ::set-page
  (fn-traced [{db :db} [_ new-page]]
    {:db (assoc db :current-page new-page)
     :dispatch [::load-stories]}))


(rf/reg-event-fx
  ::upvote-story
  (fn-traced [{db :db} [_ story-id]]
    {:http-xhrio {:method :post
                  :uri (str "http://localhost:3000/stories/" story-id "/votes")
                  :body {}
                  :response-format (ajax/raw-response-format)
                  :on-success [::load-stories] 
                  :on-failure [::xhr-failure]}}))


(rf/reg-event-fx
  ::view-story
  (fn-traced [{db :db} [_ story-id]]
    {:db (assoc db :current-story story-id)
     :dispatch [::set-page :comments]
     :http-xhrio {:method :get
                  :uri (str "http://localhost:3000/stories/" story-id "/comments")
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success [::received-comments story-id] 
                  :on-failure [::xhr-failure]}}))


(rf/reg-event-db
  ::received-comments
  (fn-traced [db [_ story-id comments]]
    (assoc-in db [:comments story-id] comments)))
             

(rf/reg-event-fx
  ::submit-comment
  (fn-traced [_ [_ story-id new-comment]]
    {:http-xhrio {:method :post
                  :uri (str "http://localhost:3000/stories/" story-id "/comments")
                  :params {:comment new-comment}
                  :format (ajax/json-request-format)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success [::comment-submitted story-id] 
                  :on-failure [::xhr-failure]}}))


(rf/reg-event-fx
  ::comment-submitted
  (fn-traced [_ [_ story-id _]] 
    {:dispatch [::view-story story-id]}))


(rf/reg-event-fx
  ::upvote-comment
  (fn-traced [{db :db} [_ story-id comment-id]]
    {:http-xhrio {:method :post
                  :uri (str "http://localhost:3000/stories/" story-id "/comments/" comment-id "/votes")
                  :body {}
                  :response-format (ajax/raw-response-format)
                  :on-success [::view-story story-id] 
                  :on-failure [::xhr-failure]}}))

  
(rf/reg-event-fx
  ::xhr-failure
  (fn-traced [_ _]
    (println "failed")
    {}))


;; Subscriptions

(rf/reg-sub
  ::current-page
  (fn [db]
    (:current-page db)))


(rf/reg-sub
  ::stories
  (fn [db]
    (:stories db)))


(rf/reg-sub
  ::current-story
  (fn [db]
    (:current-story db)))


(rf/reg-sub
  ::stories-by-id
  (fn [_ _]
      (rf/subscribe [::stories]))
  (fn [stories _]
    (->> (group-by :id stories)
         (map (fn [[k v]] [k (first v)]))
         (into {}))))


(rf/reg-sub
  ::selected-story
  (fn [_ _]  
    [(rf/subscribe [::stories-by-id])
     (rf/subscribe [::current-story])])
  (fn [[stories current-story]]
    (get stories current-story)))


(rf/reg-sub
  ::comments
  (fn [db]
    (:comments db)))


(rf/reg-sub
  ::story-comments
  (fn [_ _]  
    [(rf/subscribe [::comments])
     (rf/subscribe [::selected-story])])
  (fn [[comments {story-id :id}]]
    (get comments story-id)))


;; Views

(defn stories-page
  []
  (let [stories @(rf/subscribe [::stories])]
    [:div
      {:style {:margin "1em"
               :display "flex"
               :flex-direction "column"}}
      (map
        (fn [index {:keys [id title url votes]}]
          [:div
            {:key id
             :style {:color "#828282"
                     :display "flex"}}
            index "."
            [:a {:style {:color "#828282"}
                 :on-click #(rf/dispatch [::upvote-story id])} "⯅"]
            [:div
              [:div
                [:a
                  {:href url
                   :style {:color "#000"
                           :font-size "11pt"
                           :line-height "14pt"}}
                  title]]
              [:div
                {:style {:color "#828282"
                         :font-size "9pt"
                         :text-decoration "none"}} 
                votes " points | "
                [:a
                  {:on-click #(rf/dispatch [::view-story id])
                   :style {:color "#828282"
                           :font-size "9pt"}}
                  "comments"]]]])
        (range)
        stories)]))


(defn new-story-page
  []
  (let [title (r/atom nil)
        url (r/atom nil)]
    [:div
      {:style {:max-width "800px"
               :display "grid"
               :grid-template-columns "2.5em auto"
               :grid-template-rows "repeat(3, 1fr)"
               :grid-row-gap ".5em"
               :align-items "center"
               :margin "1em"}}
      "title"
      [:> blue/InputGroup
        {:on-change #(reset! title (.. % -target -value))}]
      "url"
      [:> blue/InputGroup
        {:on-change #(reset! url (.. % -target -value))}]
      [:> blue/Button {:style {:grid-column "2 / 3"
                               :justify-self "end"}
                       :intent "primary"
                       :text "submit"
                       :on-click #(rf/dispatch [::submit-story {:title @title
                                                                :link @url}])}]]))
   

(defn comments-page
  []
  (let [current-story @(rf/subscribe [::selected-story])
        comments @(rf/subscribe [::story-comments])
        new-comment (r/atom nil)]
    [:div
      {:style {:margin "1em"
               :display "flex"
               :flex-direction "column"}}
      [:> blue/InputGroup
        {:style {:max-width "800px"}
         :on-change #(reset! new-comment (.. % -target -value))}]
      [:div
        {:style {:display "flex"
                 :max-width "800px"
                 :justify-content "flex-end"
                 :padding-top ".5em"
                 :padding-bottom "1em"}}
        [:> blue/Button {:intent "primary"
                         :text "add comment"
                         :on-click #(do
                                      (rf/dispatch [::submit-comment {:title @new-comment}])
                                      (reset! new-comment nil))}]]
      (if (seq comments)
        (map
          (fn [index {:keys [id votes] text :comment}]
            [:div
              {:key id
               :style {:color "#828282"
                       :display "flex"}}
              index "." 
              [:a {:style {:color "#828282"}
                   :on-click #(rf/dispatch [::upvote-comment (:id current-story) id])} "⯅"]
              [:div
                [:div
                  {:style {:color "#000"
                           :font-size "11pt"
                           :line-height "14pt"}}
                  text]
                [:div
                  {:style {:color "#828282"
                           :font-size "9pt"
                           :text-decoration "none"}} 
                  votes " points"]]])
          (range)
          comments)
        [:div "No comments yet"])]))


(defn root-view
  []
  (let [current-page @(rf/subscribe [::current-page])]
    [:div
      [:div
        {:style {:width "100%"
                 :height "2.5em"
                 :padding "1em"
                 :background-color "orange"
                 :display "flex"
                 :align-items "center"}}
        [:b
          {:style {:font-size "15px"
                   :padding-right "1em"}}
          "Hacker Stories"]
        [:a 
          {:style  {:text-decoration "none"}
           :on-click #(rf/dispatch [::set-page :stories])}
          "Stories"]
        [:div
          {:style {:padding-left ".5em"
                   :padding-right ".5em"}}
          "|"]
        [:a
          {:style  {:text-decoration "none"}
           :on-click #(rf/dispatch [::set-page :new-story])}
          "Submit"]]
      (case current-page
        :stories [stories-page]
        :new-story [new-story-page]
        :comments [comments-page]
        [:div "Invalid Page"])]))

