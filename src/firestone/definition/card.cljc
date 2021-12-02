(ns firestone.definition.card
  (:require [ysera.error :refer [error]]
            [firestone.definitions :refer [add-definitions!]]
            [firestone.core :refer [damage-random
                                    deal-damages
                                    give-minion-plus-attack-and-health
                                    restore-health
                                    update-armor
                                    update-attack]]
            [firestone.construct :refer [add-card-to-hand
                                         add-specific-cards-to-hand
                                         create-card
                                         draw-card
                                         draw-for-each-damaged
                                         friendly?
                                         get-armor
                                         get-attack
                                         get-character
                                         get-hero-id-from-player-id
                                         get-minions
                                         get-opposing-player-id
                                         get-owner-id
                                         get-player-id-in-turn
                                         set-divine-shield
                                         update-minion]]))

(def card-definitions
  {

   "Argent Protector"
   {:description "Battlecry: Give a friendly minion Divine Shield."
    :name        "Argent Protector"
    :type        :minion
    :mana-cost   2
    :class       :paladin
    :health      2
    :set         :classic
    :rarity      :common
    :attack      2
    :battlecry   (fn [state other-args]
                   (let [played-card (:played-card other-args)
                         target-minion-id (:target-id other-args)]
                     (if-not (friendly? state (:id played-card) target-minion-id)
                       (error "invalid target")
                       (set-divine-shield state target-minion-id))))}

   "Argent Squire"
   {:attack      1
    :description "Divine Shield"
    :health      1
    :mana-cost   1
    :name        "Argent Squire"
    :rarity      :common
    :set         :classic
    :type        :minion
    :battlecry   (fn [state other-args]
                   (let [card (:played-card other-args)
                         target-minion-id (:id card)]
                     (set-divine-shield state target-minion-id)))} ;TODO not battlecry

   "Armorsmith"
   {:description                "Whenever a friendly minion takes damage gain 1 Armor."
    :name                       "Armorsmith"
    :type                       :minion
    :mana-cost                  2
    :class                      :warrior
    :health                     4
    :set                        :classic
    :rarity                     :rare
    :attack                     1
    :effect-minion-takes-damage (fn [state other-args]
                                  (let [minion-play-effect-id (:id (:minion-play-effect other-args))
                                        minion-takes-damage-id (:id (:minion-takes-damage other-args))]
                                    (if (friendly? state minion-play-effect-id minion-takes-damage-id) ;test if it is a friendly minion
                                      (update-armor state (get-owner-id state minion-takes-damage-id) 1)
                                      state)))}


   "Bananas"
   {:description  "Give a minion +1/+1."
    :mana-cost    1
    :name         "Bananas"
    :set          :classic
    :type         :spell
    :effect-spell (fn [state other-args]
                    (let [target-minion-id (:target-id other-args)]
                      (give-minion-plus-attack-and-health state target-minion-id 1)))}

   "Battle Rage"
   {:class        :warrior
    :description  "Draw a card for each damaged friendly character."
    :mana-cost    2
    :name         "Battle Rage"
    :rarity       :common
    :set          :classic
    :type         :spell
    :effect-spell (fn [state other-args]
                    (let [card (:spell-played other-args)
                          player-id (:owner-id card)]
                      (draw-for-each-damaged state player-id)))}

   "Blessed Champion"
   {:class        :paladin
    :description  "Double a minion's Attack."
    :mana-cost    5
    :name         "Blessed Champion"
    :rarity       :rare
    :set          :classic
    :type         :spell
    :effect-spell (fn [state other-args]
                    (let [target-minion-id (:target-id other-args)
                          attack (get-attack state target-minion-id)]
                      (-> state
                          (update-attack target-minion-id attack))))}

   "Defender"
   {:name      "Defender"
    :attack    2
    :health    1
    :mana-cost 1
    :set       :classic
    :class     :paladin
    :type      :minion
    :rarity    :common}

   "Earthen Ring Farseer"
   {:attack      3
    :description "Battlecry: Restore 3 Health."
    :health      3
    :mana-cost   3
    :name        "Earthen Ring Farseer"
    :rarity      :common
    :set         :classic
    :type        :minion
    :battlecry   (fn [state other-args]
                   (let [target-id (:target-id other-args)]
                     (restore-health state target-id 3)))}

   "King Mukla"
   {:attack      5
    :description "Battlecry: Give your opponent 2 Bananas."
    :health      5
    :mana-cost   3
    :name        "King Mukla"
    :rarity      :legendary
    :set         :classic
    :type        :minion
    :battlecry   (fn [state other-args]
                   (let [card (:played-card other-args)
                         opponent-player-id (get-opposing-player-id state (:owner-id card))]
                     (add-specific-cards-to-hand state opponent-player-id "Bananas" 2)))}

   "Knife Juggler"
   {:attack               3
    :description          "After you summon a minion, deal 1 damage to a random enemy."
    :health               2
    :mana-cost            2
    :name                 "Knife Juggler"
    :rarity               :rare
    :set                  :classic
    :type                 :minion
    :effect-summon-minion (fn [state other-args]
                            (let [minion-play-effect-id (:id (:minion-play-effect other-args))
                                  enemy-id (get-opposing-player-id state (get-owner-id state minion-play-effect-id))]
                              (damage-random state 1 enemy-id)))
    }

   "Lorewalker Cho"
   {:attack            0
    :description       "Whenever a player casts a spell, put a copy into the other player's hand."
    :health            4
    :mana-cost         2
    :name              "Lorewalker Cho"
    :rarity            :legendary
    :set               :classic
    :type              :minion
    :effect-cast-spell (fn [state rest]
                         (let [card-spell-casted (:card-spell-casted rest)
                               owner-id (:owner-id card-spell-casted)
                               opposing-id (get-opposing-player-id state owner-id)]
                           (add-card-to-hand state opposing-id (create-card (:name card-spell-casted))))) ;could be improved, copying the stats of the card.
    }

   "Novice Engineer"
   {:name        "Novice Engineer"
    :attack      1
    :health      1
    :mana-cost   2
    :type        :minion
    :set         :basic
    :description "Battlecry: Draw a card."
    :battlecry   (fn [state other-args]
                   (draw-card state (get-player-id-in-turn state)))}

   "Nightblade"
   {:name        "Nightblade"
    :attack      4
    :health      4
    :mana-cost   5
    :type        :minion
    :set         :basic
    :description "Battlecry: Deal 3 damage to the enemy hero."
    :battlecry   (fn [state other-args]
                   (deal-damages state (get-opposing-player-id state) 3))}

   "Ragnaros the Firelord"
   {:attack             8
    :description        "Can't attack. At the end of your turn, deal 8 damage to a random enemy."
    :health             8
    :mana-cost          8
    :name               "Ragnaros the Firelord"
    :rarity             :legendary
    :set                :hall-of-fame
    :type               :minion
    :effect-cant-attack true
    :effect-end-turn    (fn [state other-args]
                          (let [minion-play-effect-id (:id (:minion-play-effect other-args))
                                enemy-id (get-opposing-player-id state (get-owner-id state minion-play-effect-id))]
                            (damage-random state 8 enemy-id)))}

   "Shield Slam"
   {:class        :warrior
    :description  "Deal 1 damage to a minion for each Armor you have."
    :mana-cost    1
    :name         "Shield Slam"
    :rarity       :epic
    :set          :classic
    :type         :spell
    :effect-spell (fn [state other-args]
                    (let [card (:spell-played other-args)
                          target-minion-id (:target-id other-args)
                          owner-id (get-in card [:owner-id])
                          number-armor (get-armor state (get-hero-id-from-player-id state owner-id))]
                      (deal-damages state target-minion-id number-armor)))}

   "Snake"
   {:name      "Snake"
    :attack    1
    :health    1
    :mana-cost 1
    :type      :minion
    :set       :classic
    :race      :beast
    :class     :hunter}

   "Whirlwind"
   {:class        :warrior
    :description  "Deal one damage to ALL minions."
    :mana-cost    1
    :name         "Whirlwind"
    :set          :basic
    :type         :spell
    :effect-spell (fn [state other-args]
                    (let [minions-list (get-minions state)
                          deal-one-damage (fn [s minion] (deal-damages s (:id minion) 1))]
                      (reduce deal-one-damage state minions-list)))
    }})
(add-definitions! card-definitions)