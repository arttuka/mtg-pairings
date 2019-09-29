(ns mtg-pairings-server.util.decklist
  (:require [clojure.string :as str]
            [mtg-pairings-server.util :as util]))

(defn types->keyword-set [card]
  (update card :types #(set (map (comp keyword str/lower-case) %))))

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

(defn decklist-url [id]
  (str "https://decklist.pairings.fi/" id))

(defn pad [n]
  (if (< n 10)
    (str " " n)
    (str n)))

(defn ->text [decklist]
  (let [{:keys [main side]} decklist]
    (str "Maindeck (" (get-in decklist [:count :main]) ")\n"
         (str/join "\n" (for [{:keys [name quantity]} main]
                          (str (pad quantity) " " name)))
         "\n\nSideboard (" (get-in decklist [:count :side]) ")\n"
         (str/join "\n" (for [{:keys [name quantity]} side]
                          (str (pad quantity) " " name)))
         "\n")))

(def card-types
  [:creature
   :artifact
   :enchantment
   :instant
   :sorcery
   :planeswalker
   :land
   :error])

(defn by-type [decklist]
  (let [{:keys [main]} decklist
        cards-by-type (loop [cards main
                             parts {}
                             [type & types] card-types]
                        (cond
                          (empty? cards) parts
                          (nil? type) (assoc parts :error cards)
                          :else (let [[matching-cards other-cards] (util/separate #(contains? (:types %) type) cards)]
                                  (recur other-cards
                                         (assoc parts type (seq matching-cards))
                                         types))))]
    (assoc decklist :main cards-by-type)))
