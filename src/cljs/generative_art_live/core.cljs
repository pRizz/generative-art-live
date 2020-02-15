(ns generative-art-live.core
  (:require
    [reagent.core :as reagent :refer [atom]]
    [reagent.session :as session]
    [reitit.frontend :as reitit]
    [clerk.core :as clerk]
    [accountant.core :as accountant]
    [brickhack.ribbons :as ribbons]))

;; -------------------------
;; Routes

(def router
  (reitit/router
    [["/" :index]
     ["/items"
      ["" :items]
      ["/:item-id" :item]]
     ["/about" :about]]))

(defn path-for [route & [params]]
  (if params
    (:path (reitit/match-by-name router route params))
    (:path (reitit/match-by-name router route))))

(path-for :about)
;; -------------------------
;; Page components

(defn home-page []
  (fn []
    [:span.main
     [:h1 "Welcome to generative-art-live"]
     [:ul
      [:li [:a {:href (path-for :items)} "Items of generative-art-live"]]
      [:li [:a {:href "/broken/link"} "Broken link"]]]]))



(defn items-page []
  (fn []
    [:span.main
     [:h1 "The items of generative-art-live"]
     [:ul (map (fn [item-id]
                 [:li {:name (str "item-" item-id) :key (str "item-" item-id)}
                  [:a {:href (path-for :item {:item-id item-id})} "Item: " item-id]])
               (range 1 60))]]))


(defn item-page []
  (fn []
    (let [routing-data (session/get :route)
          item (get-in routing-data [:route-params :item-id])]
      [:span.main
       [:h1 (str "Item " item " of generative-art-live")]
       [:p [:a {:href (path-for :items)} "Back to the list of items"]]])))


(defn about-page []
  (fn [] [:span.main
          [:h1 "About generative-art-live"]]))


;; -------------------------
;; Translate routes -> page components

(defn page-for [route]
  (case route
    :index #'home-page
    :about #'about-page
    :items #'items-page
    :item #'item-page))


;; -------------------------
;; Page mounting component

(defn- toolbar-element []
  [:div {:style {:position         "absolute"
                 :top              20
                 :right            20
                 :padding          20
                 :color            "#ddd"
                 :background-color "black"
                 :borderRadius     8}}
   "testing"])

(defn- sketch-element []
  [:div#sketch {:style {:position         "absolute"
                        :top              0
                        :bottom           0
                        :left             0
                        :right            0
                        :background-color "black"}}])

(defn current-page []
  (let [seed 1234
        sketch-instance (js/setTimeout #(ribbons/sketch {:canvas-id "sketch"
                                                         :seed      seed})
                                       500)])               ; TODO: find a cleaner way to wait for sketch div to render
  (fn []
    [:<>
     [sketch-element]
     [toolbar-element]]))

;; -------------------------
;; Initialize app

(defn mount-root []
  (reagent/render [current-page] (.getElementById js/document "app")))

(defn init! []
  (clerk/initialize!)
  (accountant/configure-navigation!
    {:nav-handler
     (fn [path]
       (let [match (reitit/match-by-path router path)
             current-page (:name (:data match))
             route-params (:path-params match)]
         (reagent/after-render clerk/after-render!)
         (session/put! :route {:current-page (page-for current-page)
                               :route-params route-params})
         (clerk/navigate-page! path)))

     :path-exists?
     (fn [path]
       (boolean (reitit/match-by-path router path)))})
  (accountant/dispatch-current!)
  (mount-root)
  (aset (-> js/document .-documentElement .-style) "backgroundColor" "black"))
