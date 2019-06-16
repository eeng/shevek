(ns shevek.components.virtualized-examples
  (:require [shevek.components.virtualized :refer [virtual-table]]
            [clojure.string :as str]
            [shevek.components.benchmark :refer [benchmark]]))

(def lorem "Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to make a type specimen book. It has survived not only five centuries, but also the leap into electronic typesetting, remaining essentially unchanged. It was popularised in the 1960s with the release of Letraset sheets containing Lorem Ipsum passages, and more recently with desktop publishing software like Aldus PageMaker including versions of Lorem Ipsum.")

(defn example-page []
  (let [rows 100
        columns 3
        rand-str #(-> lorem (str/split #" ") rand-nth)
        data (vec (repeatedly rows #(vec (repeatedly columns rand-str))))]
    [benchmark
     [:div#example-page {:style {:width "500px" :height "400px" :margin "3em" :background-color "#ffff0055"}}
      [virtual-table
       {:class "pivot-table"
        :height 400 ; TODO RV try to autodetect, needed for the overflow
        :row-height 40
        :window-buffer 3
        :header-count 1
        :header-renderer
        (fn [{:keys [row-idx]}]
          [:tr
           (for [col-idx (range columns)]
             [:th {:key col-idx} (str "Header " row-idx "-" col-idx)])])
        :row-count rows
        :row-renderer
        (fn [{:keys [row-idx style]}]
          [:tr {:key row-idx}
           (for [[col-idx cell] (map-indexed vector (get data row-idx))]
             [:td {:key col-idx :style (merge style {:background-color "#b4bcf1" :padding ".5em"})}
              (str row-idx ": " cell)])])}]]]))
