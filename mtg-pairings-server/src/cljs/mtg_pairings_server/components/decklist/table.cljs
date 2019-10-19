(ns mtg-pairings-server.components.decklist.table)

(defn table-cell-style [styles]
  (merge {:font-size 14
          :padding   0}
         styles))

(defn table-styles [{:keys [palette spacing]}]
  {:link              {:display     :block
                       :color       (get-in palette [:text :primary])
                       :line-height "48px"
                       :padding     (spacing 0 2)}
   :table-header-cell {:font-weight :bold
                       :font-size   "16px"}})
