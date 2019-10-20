(ns mtg-pairings-server.util.decklist
  (:require [clojure.string :as str]
            [mtg-pairings-server.util :as util]
            [mtg-pairings-server.util.mtg :refer [valid-dci?]]))

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

(def basic? #{"Plains" "Island" "Swamp" "Mountain" "Forest" "Wastes"
              "Snow-Covered Plains" "Snow-Covered Island" "Snow-Covered Swamp"
              "Snow-Covered Mountain" "Snow-Covered Forest"})

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

(defn valid-player-data? [{:keys [first-name last-name dci]}]
  (and (valid-dci? dci)
       (not (str/blank? first-name))
       (not (str/blank? last-name))))

(defn decklist-errors [decklist]
  (let [all-cards (concat (mapcat (:main decklist) card-types) (:side decklist))
        cards (reduce (fn [acc {:keys [name quantity]}]
                        (merge-with + acc {name quantity}))
                      {}
                      all-cards)
        errors (concat [(when-not (valid-player-data? (:player decklist)) {:type :player-data
                                                                           :id   :missing-player-data})
                        (when (< (get-in decklist [:count :main]) 60) {:type :maindeck
                                                                       :id   :deck-error-maindeck})
                        (when (> (get-in decklist [:count :side]) 15) {:type :sideboard
                                                                       :id   :deck-error-sideboard})]
                       (for [[card quantity] cards
                             :when (not (basic? card))
                             :when (> quantity 4)]
                         {:type :card-over-4
                          :id   (str "deck-error-card--" card)
                          :card card
                          :text :card-over-4})
                       (for [{:keys [error name]} all-cards
                             :when error]
                         {:type :other
                          :id   (str "other-error-card--" name)
                          :text error
                          :card name}))]
    (filter some? errors)))
