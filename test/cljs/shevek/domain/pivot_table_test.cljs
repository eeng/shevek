(ns shevek.domain.pivot-table-test
  (:require [cljs.test :refer-macros [deftest testing is use-fixtures]]
            [shevek.domain.pivot-table :refer [generate empty-cell measure-cell grand-total-cell measure-value-cell dimension-value-cell splits-cell]]
            [com.rpl.specter :refer [transform ALL select]]))

(def sales {:name "sales" :title "Sales"})
(def taxes {:name "taxes" :title "Taxes"})
(def country {:name "country" :title "Country"})
(def city {:name "city" :title "City"})
(def isNew {:name "isNew" :title "Is New" :type "BOOL"})

(defn only-important-fields
  "Allows to write simpler tests without non-essential fields"
  [{:keys [proportion splits top-left-corner in-columns participation] :as cell}]
  (cond-> cell
          proportion (assoc :proportion 0)
          participation (assoc :participation 0)
          splits (assoc :splits [])
          top-left-corner (assoc :top-left-corner false)
          in-columns (assoc :in-columns false)))

(defn head [pivot-table]
  (->> (:head pivot-table)
       (transform [ALL ALL] only-important-fields)))

(defn body [pivot-table]
  (->> pivot-table
       (select [:body ALL :cells])
       (transform [ALL ALL] only-important-fields)))

(deftest pivot-table-test
  (testing "totals only"
    (let [r {:sales 10, :taxes 20}
          t (generate {:measures [sales taxes] :results [r]})]
      (is (= [[(empty-cell) (measure-cell sales) (measure-cell taxes)]]
             (head t)))
      (is (= [[(grand-total-cell) (measure-value-cell sales r) (measure-value-cell taxes r)]]
             (body t)))))

  (testing "one row dimension and one measure"
    (let [[r9 r7 r2 :as results] [{:sales 9}
                                  {:sales 7 :country "FR"}
                                  {:sales 2 :country "IT"}]
          t (generate {:splits [country] :measures [sales] :results results})]
      (is (= [[(splits-cell [country]) (measure-cell sales)]]
             (head t)))
      (is (= [[(grand-total-cell) (measure-value-cell sales r9)]
              [(dimension-value-cell country r7) (measure-value-cell sales r7)]
              [(dimension-value-cell country r2) (measure-value-cell sales r2)]]
             (body t)))))

  (testing "one row dimension and two measures"
    (let [[r9 r7 r2 :as results] [{:sales 99 :taxes 9}
                                  {:sales 77 :taxes 7 :country "FR"}
                                  {:sales 22 :taxes 2 :country "IT"}]
          t (generate {:splits [country] :measures [sales taxes] :results results})]
      (is (= [[(splits-cell [country]) (measure-cell sales) (measure-cell taxes)]]
             (head t)))
      (is (= [[(grand-total-cell) (measure-value-cell sales r9) (measure-value-cell taxes r9)]
              [(dimension-value-cell country r7) (measure-value-cell sales r7) (measure-value-cell taxes r7)]
              [(dimension-value-cell country r2) (measure-value-cell sales r2) (measure-value-cell taxes r2)]]
             (body t)))))

  (testing "two row dimensions and one measure"
    (let [[r11 r12] [{:sales 4 :city "cba"} {:sales 3 :city "sfe"}]
          r21 {:sales 2 :city "sgo"}
          [r0 r1 r2 :as results] [{:sales 9}
                                  {:sales 7 :country "AR" :child-rows [r11 r12]}
                                  {:sales 2 :country "CH" :child-rows [r21]}]
          t (generate {:splits [country city] :measures [sales] :results results})]
      (is (= [[(splits-cell [country, city]) (measure-cell sales)]]
             (head t)))
      (is (= [[(grand-total-cell) (measure-value-cell sales r0)]
              [(dimension-value-cell country r1 :depth 0) (measure-value-cell sales r1)]
              [(dimension-value-cell city r11 :depth 1) (measure-value-cell sales r11)]
              [(dimension-value-cell city r12 :depth 1) (measure-value-cell sales r12)]
              [(dimension-value-cell country r2 :depth 0) (measure-value-cell sales r2)]
              [(dimension-value-cell city r21 :depth 1) (measure-value-cell sales r21)]]
             (body t)))))

  (testing "one column dimension and one measure"
    (let [country (assoc country :on "columns")
          r1 {:sales 5 :country "AR"}
          r2 {:sales 2 :country "CH"}
          r {:sales 7 :child-cols [r1 r2]}
          t (generate {:splits [country] :measures [sales] :results [r]})]
      (is (= [[(measure-cell sales)
               (splits-cell [country] :col-span 3)]
              [(empty-cell)
               (dimension-value-cell country r1)
               (dimension-value-cell country r2)
               (grand-total-cell)]]
             (head t)))
      (is (= [[(grand-total-cell)
               (measure-value-cell sales r1)
               (measure-value-cell sales r2)
               (measure-value-cell sales r)]]
             (body t)))))

  (testing "two column dimensions and one measure"
    (let [country (assoc country :on "columns")
          isNew (assoc isNew :on "columns")
          [r11 r12] [{:isNew "Yes" :sales 1} {:isNew "No" :sales 4}]
          [r21] [{:isNew "No" :sales 2}]
          r1 {:sales 5 :country "AR" :child-cols [r11 r12]}
          r2 {:sales 2 :country "CH" :child-cols [r21]}
          r {:sales 7 :child-cols [r1 r2]}
          t (generate {:splits [country isNew] :measures [sales] :results [r]})
          the-head (head t)]
      (is (= [(measure-cell sales)
              (splits-cell [country isNew] :col-span 6)]
             (nth the-head 0)))
      (is (= [(empty-cell)
              (dimension-value-cell country r1 :col-span 3)
              (dimension-value-cell country r2 :col-span 2)
              (empty-cell)]
             (nth the-head 1)))
      (is (= [(empty-cell)
              (dimension-value-cell isNew r11)
              (dimension-value-cell isNew r12)
              (dimension-value-cell isNew r1)
              (dimension-value-cell isNew r21)
              (dimension-value-cell isNew r2)
              (grand-total-cell)]
             (nth the-head 2)))
      (is (= [[(grand-total-cell)
               (measure-value-cell sales r11)
               (measure-value-cell sales r12)
               (measure-value-cell sales r1)
               (measure-value-cell sales r21)
               (measure-value-cell sales r2)
               (measure-value-cell sales r)]]
             (body t)))))

  (testing "two column dimensions and two measures",
    (let [country (assoc country :on "columns")
          isNew (assoc isNew :on "columns")
          [r11 r12] [{:isNew "Yes" :sales 1 :taxes 11} {:isNew "No" :sales 4 :taxes 44}]
          [r21] [{:isNew "No" :sales 2 :taxes 22}]
          r1 {:sales 5 :taxes 55 :country "AR" :child-cols [r11 r12]}
          r2 {:sales 2 :taxes 22 :country "CH" :child-cols [r21]}
          r {:sales 7 :taxes 77 :child-cols [r1 r2]}
          t (generate {:splits [country isNew] :measures [sales taxes] :results [r]})
          the-head (head t)]
      (is (= [(empty-cell) (splits-cell [country isNew] :col-span 12)]
             (nth the-head 0)))
      (is (= [(empty-cell)
              (dimension-value-cell country r1 :col-span 6)
              (dimension-value-cell country r2 :col-span 4)
              (empty-cell)]
             (nth the-head 1)))
      (is (= [(empty-cell)
              (dimension-value-cell isNew r11 :col-span 2)
              (dimension-value-cell isNew r12 :col-span 2)
              (dimension-value-cell isNew r1 :col-span 2)
              (dimension-value-cell isNew r21 :col-span 2)
              (dimension-value-cell isNew r2 :col-span 2)
              (grand-total-cell isNew r :col-span 2)]
             (nth the-head 2)))
      (is (= [(empty-cell)
              (measure-cell sales) (measure-cell taxes)
              (measure-cell sales) (measure-cell taxes)
              (measure-cell sales) (measure-cell taxes)
              (measure-cell sales) (measure-cell taxes)
              (measure-cell sales) (measure-cell taxes)
              (measure-cell sales) (measure-cell taxes)]
             (nth the-head 3)))
      (is (= [[(grand-total-cell)
               (measure-value-cell sales r11) (measure-value-cell taxes r11)
               (measure-value-cell sales r12) (measure-value-cell taxes r12)
               (measure-value-cell sales r1) (measure-value-cell taxes r1)
               (measure-value-cell sales r21) (measure-value-cell taxes r21)
               (measure-value-cell sales r2) (measure-value-cell taxes r2)
               (measure-value-cell sales r) (measure-value-cell taxes r)]]
             (body t)))))

  (testing "two row dimensions, one column dimension and one measure"
    (let [isNew (assoc isNew :on "columns")
          [r01 r02] [{:sales 7 :isNew "No"} {:sales 3 :isNew "Yes"}]
          r0 {:sales 10 :child-cols [r01 r02]}
          [r1c1 r1c2] [{:sales 7 :isNew "No"} {:sales 1 :isNew "Yes"}]
          r1r1c2 {:sales 1 :isNew "Yes"}
          r1r2c1 {:sales 3 :isNew "No"}
          r1r1 {:sales 5 :city "cba" :child-cols [r1r1c2]}
          r1r2 {:sales 3 :city "sfe" :child-cols [r1r2c1]}
          r1 {:sales 8 :country "AR"
              :child-cols [r1c1 r1c2]
              :child-rows [r1r1 r1r2]}
          t (generate {:splits [country city isNew] :measures [sales] :results [r0 r1]})
          the-body (body t)]
      (is (= [[(measure-cell sales) (splits-cell [isNew] :col-span 3)]
              [(splits-cell [country, city] :col-span 1)
               (dimension-value-cell isNew r01)
               (dimension-value-cell isNew r02)
               (grand-total-cell isNew r0)]]
             (head t)))
      (is (= [(grand-total-cell)
              (measure-value-cell sales r01)
              (measure-value-cell sales r02)
              (measure-value-cell sales r0)]
             (nth the-body 0)))
      (is (= [(dimension-value-cell country r1)
              (measure-value-cell sales r1c1)
              (measure-value-cell sales r1c2)
              (measure-value-cell sales r1)]
             (nth the-body 1)))
      (is (= [(dimension-value-cell city r1r1 :depth 1)
              (measure-value-cell sales {:city "cba"})
              (measure-value-cell sales r1r1c2)
              (measure-value-cell sales r1r1)]
             (nth the-body 2)))
      (is (= [(dimension-value-cell city r1r2 :depth 1)
              (measure-value-cell sales r1r2c1)
              (measure-value-cell sales {:city "sfe"})
              (measure-value-cell sales r1r2)]
             (nth the-body 3))))))
