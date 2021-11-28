(ns firestone.definition.card
  (:require [ysera.error :refer [error]]
            [ysera.random :refer [random-nth]]
            [firestone.definitions :refer [add-definitions!]]
            [firestone.core :refer [deal-damages
                                    damage-random-enemy
                                    get-armor
                                    get-attack
                                    get-character
                                    get-hero-id-from-player-id
                                    update-armor
                                    update-attack]]
            [firestone.construct :refer [draw-card
                                         draw-for-each-damaged
                                         get-minions
                                         get-opposing-player-id
                                         get-player-id-in-turn
                                         set-divine-shield
                                         give-minion-plus-one
                                         draw-specific-card
                                         draw-specific-card
                                         update-minion
                                         add-card-to-hand
                                         create-card]]))

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
    :battlecry   (fn [state card target-minion-id]
                   (let [owner-id (get-in card [:owner-id])
                         valid-target-list (get-minions state owner-id)
                         is-valid-target? (reduce (fn [a v]
                                                    (if (= (:id v) target-minion-id)
                                                      true
                                                      a))
                                                  false
                                                  valid-target-list)]
                     (if-not is-valid-target?
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
    :battlecry   (fn [state card]
                   (let [target-minion-id (:id card)]
                     (set-divine-shield state target-minion-id)))}

   "Armorsmith"
   {:description                              "Whenever a friendly minion takes damage gain 1 Armor."
    :name                                     "Armorsmith"
    :type                                     :minion
    :mana-cost                                2
    :class                                    :warrior
    :health                                   4
    :set                                      :classic
    :rarity                                   :rare
    :attack                                   1
    :effect-when-friendly-minion-takes-damage (fn [state card]
                                                (let [player-id (:owner-id card)]
                                                  (update-armor state player-id 1)))}


   "Bananas"
   {:description "Give a minion +1/+1."
    :mana-cost   1
    :name        "Bananas"
    :set         :classic
    :type        :spell
    :battlecry   (fn [state card pos]
                   (let [target-minion-name (:name card)
                         target-minion-id (:id card)
                         player-id (get-player-id-in-turn state)]
                     (give-minion-plus-one state player-id target-minion-name target-minion-id pos)))}

   "Battle Rage"
   {:class       :warrior
    :description "Draw a card for each damaged friendly character."
    :mana-cost   2
    :name        "Battle Rage"
    :rarity      :common
    :set         :classic
    :type        :spell
    :battlecry   (fn [state card]
                   (let [player-id (:owner-id card)]
                     (draw-for-each-damaged state player-id)))}

   "Blessed Champion"
   {:class       :paladin
    :description "Double a minion's Attack."
    :mana-cost   5
    :name        "Blessed Champion"
    :rarity      :rare
    :set         :classic
    :type        :spell
    :battlecry   (fn [state card target-minion-id]
                   (let [attack (get-attack state target-minion-id)]
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
    :battlecry   (fn [state card]
                   (let [owner-id (get-in card [:owner-id])]
                     (-> state
                         (update-armor owner-id 3))))}

   "King Mukla"
   {:attack      5
    :description "Battlecry: Give your opponent 2 Bananas."
    :health      5
    :mana-cost   3
    :name        "King Mukla"
    :rarity      :legendary
    :set         :classic
    :type        :minion
    :battlecry   (fn [state]
                   (let [player-id (get-opposing-player-id state)]
                     (draw-specific-card state player-id "Bananas" 2)))}

   "Knife Juggler"
   {:attack      3
    :description "After you summon a minion, deal 1 damage to a random enemy."
    :health      2
    :mana-cost   2
    :name        "Knife Juggler"
    :rarity      :rare
    :set         :classic
    :type        :minion
    :summon-friendly-minion-do-attack-spell (fn [state]
                                              (let [enemy-id (get-opposing-player-id state)]
                                                (damage-random-enemy state enemy-id)))
    }

   "Lorewalker Cho"
   {:attack      0
    :description "Whenever a player casts a spell, put a copy into the other player's hand."
    :health      4
    :mana-cost   2
    :name        "Lorewalker Cho"
    :rarity      :legendary
    :set         :classic
    :type        :minion
    :copy-spell-card-to-opposite-player (fn [state card]
                                          (let [opposing-id (get-opposing-player-id state)]
                                            (add-card-to-hand state opposing-id (create-card card))))
    }

   "Novice Engineer"
   {:name        "Novice Engineer"
    :attack      1
    :health      1
    :mana-cost   2
    :type        :minion
    :set         :basic
    :description "Battlecry: Draw a card."
    :battlecry   (fn [state card]
                   (draw-card state (get-player-id-in-turn state)))}

   "Nightblade"
   {:name        "Nightblade"
    :attack      4
    :health      4
    :mana-cost   5
    :type        :minion
    :set         :basic
    :description "Battlecry: Deal 3 damage to the enemy hero."
    :battlecry   (fn [state card]
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
    :effect-end-turn    (fn [state card]
                          (let [player-id ({"p1" "p2"
                                            "p2" "p1"} (:owner-id card))
                                minion-list (get-minions state player-id)
                                minion-and-hero-list (conj minion-list (get-character state (get-hero-id-from-player-id state player-id)))
                                random-character-id (:id ((random-nth 1 minion-and-hero-list) 1))]
                            (deal-damages state random-character-id 8)))}

   "Shield Slam"
   {:class       :warrior
    :description "Deal 1 damage to a minion for each Armor you have."
    :mana-cost   1
    :name        "Shield Slam"
    :rarity      :epic
    :set         :classic
    :type        :spell
    :battlecry   (fn [state card target-minion-id]
                   (let [owner-id (get-in card [:owner-id])
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
   {:class       :warrior
    :description "Deal one damage to ALL minions."
    :mana-cost   1
    :name        "Whirlwind"
    :set         :basic
    :type        :spell
    :battlecry   (fn [state card]
                   (let [minions-list (get-minions state)
                         deal-one-damage (fn [s minion] (deal-damages s (:id minion) 1))]
                     (reduce deal-one-damage state minions-list)))
    }})
(add-definitions! card-definitions)