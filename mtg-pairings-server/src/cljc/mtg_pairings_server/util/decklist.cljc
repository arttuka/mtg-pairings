(ns mtg-pairings-server.util.decklist)

(defn add-id-to-card
  ([card]
   (add-id-to-card "card__" card))
  ([prefix card]
   (assoc card :id (gensym prefix))))

(defn add-id-to-cards
  ([decklist]
   (add-id-to-cards "card__" decklist))
  ([prefix decklist]
   (-> decklist
       (update :main #(mapv (partial add-id-to-card prefix) %))
       (update :side #(mapv (partial add-id-to-card prefix) %)))))
