(ns shevek.components.drag-and-drop)

(def drag-data (atom nil))

(defn drag-start [event transfer-data]
  (reset! drag-data transfer-data)
  (let [drag-image (-> (js/$ "<div class='drag-image'/>")
                       (.text (.. event -target -innerText))
                       (.appendTo "body")
                       (.get 0))]
    (.. event -dataTransfer (setDragImage drag-image 5 20))))

(defn drag-end [_]
  (.remove (js/$ ".drag-image")))

(defn handle-drop [handler]
  (fn [event]
    (handler @drag-data)
    (.preventDefault event)
    (.stopPropagation event)))

(defn drag-over [event]
  (.preventDefault event))

(defn draggable [dimension]
  {:draggable true
   :on-drag-start #(drag-start % dimension)
   :on-drag-end drag-end})

(defn droppable [handler]
  {:on-drag-over drag-over
   :on-drop (handle-drop handler)})
