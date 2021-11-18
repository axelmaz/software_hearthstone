(ns firestone.definition.card
  (:require [firestone.definitions :refer [add-definitions!]]
            [firestone.core :refer []]
            [firestone.construct :refer [draw-card
                                         get-opposing-player-id
                                         get-player-id-in-turn]]))

(def card-definitions
  {

   "Defender"
   {:name      "Defender"
    :attack    2
    :health    1
    :mana-cost 1
    :set       :classic
    :class     :paladin
    :type      :minion
    :rarity    :common}

   "Nightblade"
   {:name        "Nightblade"
    :attack      4
    :health      4
    :mana-cost   5
    :type        :minion
    :set         :basic
    :description "Battlecry: Deal 3 damage to the enemy hero."
    :battlecry (fn [state]
                 (update-in state [:players (get-opposing-player-id state) :hero :damage-taken] + 3))}

   "Novice Engineer"
   {:name        "Novice Engineer"
    :attack      1
    :health      1
    :mana-cost   2
    :type        :minion
    :set         :basic
    :description "Battlecry: Draw a card."
    :battlecry (fn [state ]
                 (draw-card state (get-player-id-in-turn state)))}

   "Snake"
   {:name          "Snake"
    :attack        1
    :health        1
    :mana-cost     1
    :type          :minion
    :set           :classic
    :race          :beast
    :class         :hunter}


   })

(add-definitions! card-definitions)