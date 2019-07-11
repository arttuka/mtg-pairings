(ns mtg-pairings-server.util.decklist
  (:require [clojure.string :as str]))

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

(def type->header
  {:artifact     "Artifact"
   :creature     "Creature"
   :enchantment  "Enchantment"
   :instant      "Instant"
   :land         "Land"
   :planeswalker "Planeswalker"
   :sorcery      "Sorcery"
   :error        "Virheelliset"})

(defn by-type [decklist]
  (let [{:keys [main]} decklist
        cards-by-type (loop [cards main
                             parts {}
                             [type & types] (map type->header card-types)]
                        (if type
                          (let [grouped-cards (group-by #(contains? (:types %) type) cards)]
                            (recur (get grouped-cards false)
                                   (assoc parts (keyword (str/lower-case type)) (get grouped-cards true))
                                   types))
                          (assoc parts :error cards)))]
    (assoc decklist :main cards-by-type)))
