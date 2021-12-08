(ns firestone.definition.hero-powers
  (:require
    [firestone.core :refer [deal-damages
                            update-armor]]
    [firestone.construct :refer [get-hero-id-from-player-id
                                 get-heroes
                                 get-minions
                                 get-opposing-player-id
                                 get-player-id-in-turn]]))

(def hero-powers
  {"Armor Up!"
   {:name        "Armor Up!"
    :mana-cost   2
    :class       :warrior
    :type        :hero-power
    :description "Gain 2 Armor."
    :states      (fn [state other-args]
                   (let [player-id (get-player-id-in-turn state)]
                     (update-armor state player-id 2)))}

   "Fireblast"
   {:name         "Fireblast"
    :mana-cost    2
    :type         :hero-power
    :class        :mage
    :description  "Deal 1 damage."
    :states       (fn [state other-args]
                    (let [target-id (:target-id other-args)]
                      (deal-damages state target-id 1 {})))
    :valid-target (fn [state player-id]
                    (let [heroes-id (map :id (get-heroes state))
                          minions-id (map :id (get-minions state))]
                      (concat minions-id heroes-id)))}

   "Totemic Call"
   {:name        "Totemic Call"
    :mana-cost   2
    :type        :hero-power
    :class       :shaman
    :description "Totemic Call"}})

(firestone.definitions/add-definitions! hero-powers)