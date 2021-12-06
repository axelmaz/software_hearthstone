(ns firestone.definition.hero-powers
  (:require
    [firestone.core :refer [deal-damages
                            update-armor]]
    [firestone.construct :refer [get-player-id-in-turn
                                 get-hero-id-from-player-id]]))

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
   {:name        "Fireblast"
    :mana-cost   2
    :type        :hero-power
    :class       :mage
    :description "Deal 1 damage."
    :states (fn [state other-args]
                (let [target-id (:target-id other-args)] (deal-damages state target-id 1 {})))}

   "Totemic Call"
   {:name        "Totemic Call"
    :mana-cost   2
    :type        :hero-power
    :class       :shaman
    :description "Totemic Call"}})

(firestone.definitions/add-definitions! hero-powers)