(ns pivot.cube)

(defn page []
  [:div#cube.ui.padded.grid
   [:div.three.wide.column
    [:div.ui.segments
     [:div.ui.segment "Dimensions"]
     [:div.ui.segment "Measures"]]]
   [:div#middle-col.ten.wide.column
    [:div.ui.segments
     [:div.ui.segment "Filters"]
     [:div.ui.segment "Split"]]
    [:div.ui.segment "Content"]]
   [:div.three.wide.column
    [:div.ui.segment "Pinned"]]])
