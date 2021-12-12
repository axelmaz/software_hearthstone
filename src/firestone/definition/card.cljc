(ns firestone.definition.card
  (:require [ysera.error :refer [error]]
            [firestone.definitions :refer [add-definitions!
                                           get-definition]]
            [firestone.core :refer [damage-random
                                    deal-damages
                                    draw-card
                                    draw-for-each-damaged
                                    give-minion-plus-attack-and-health
                                    restore-health
                                    summon-minion
                                    update-armor
                                    update-attack]]
            [firestone.construct :refer [add-card-to-hand
                                         add-card-to-deck
                                         add-secret
                                         add-specific-cards-to-hand
                                         create-card
                                         create-secret
                                         friendly-when-not-on-board?
                                         friendly?
                                         get-armor
                                         get-attack
                                         get-character
                                         get-deck
                                         get-health
                                         get-hero-id-from-player-id
                                         get-minions
                                         get-opposing-player-id
                                         get-owner-id
                                         get-player-id-in-turn
                                         get-random-minion
                                         remove-all-minions
                                         remove-all-cards-from-hand
                                         set-effect
                                         shuffle-deck
                                         swap-minion-of-player
                                         update-card
                                         update-id-character
                                         update-minion]]
            [ysera.random :refer [get-random-int]]))

(def card-definitions
  {
   "Argent Protector"
   {:description  "Battlecry: Give a friendly minion Divine Shield."
    :name         "Argent Protector"
    :type         :minion
    :mana-cost    2
    :class        :paladin
    :health       2
    :set          :classic
    :rarity       :common
    :attack       2
    :battlecry    (fn [state other-args]
                    (let [played-card (:played-card other-args)
                          target-minion-id (:target-id other-args)]
                      (if (some? target-minion-id)
                        (if-not (friendly-when-not-on-board? state (:owner-id played-card) target-minion-id)
                          (error "invalid target")
                          (set-effect state target-minion-id :divine-shield))
                        state)))
    :valid-target (fn [state card]
                    (vec (map :id (get-minions state (:owner-id card)))))}

   "Argent Squire"
   {:attack      1
    :description "Divine Shield"
    :health      1
    :mana-cost   1
    :name        "Argent Squire"
    :rarity      :common
    :set         :classic
    :type        :minion
    :states      [:divine-shield]}

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
    :states                     [:effect]
    :states-minion-takes-damage (fn [state other-args]
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
    :states-spell (fn [state other-args]
                    (let [target-minion-id (:target-id other-args)]
                      (give-minion-plus-attack-and-health state target-minion-id 1)))
    :valid-target (fn [state card]
                    (vec (map :id (get-minions state))))}

   "Battle Rage"
   {:class        :warrior
    :description  "Draw a card for each damaged friendly character."
    :mana-cost    2
    :name         "Battle Rage"
    :rarity       :common
    :set          :classic
    :type         :spell
    :states-spell (fn [state other-args]
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
    :states-spell (fn [state other-args]
                    (let [target-minion-id (:target-id other-args)
                          attack (get-attack state target-minion-id)]
                      (-> state
                          (update-attack target-minion-id attack))))
    :valid-target (fn [state card]
                    (vec (map :id (get-minions state))))}

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
   {:attack       3
    :description  "Battlecry: Restore 3 Health."
    :health       3
    :mana-cost    3
    :name         "Earthen Ring Farseer"
    :rarity       :common
    :set          :classic
    :type         :minion
    :battlecry    (fn [state other-args]
                    (let [target-id (:target-id other-args)]
                      (restore-health state target-id 3)))
    :valid-target (fn [state card]
                    (conj (vec (map :id (get-minions state))) (get-hero-id-from-player-id state "p1") (get-hero-id-from-player-id state "p2")))}

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
    :states               [:effect]
    :states-summon-minion (fn [state other-args]
                            (let [minion-play-effect (:minion-play-effect other-args)
                                  minion-play-effect-id (:id minion-play-effect)
                                  owner-id-play-effect (:owner-id minion-play-effect)
                                  minion-summoned-id (:id (:minion-summoned other-args))
                                  owner-id-summoned (:player-summon other-args)
                                  enemy-id (get-opposing-player-id state owner-id-play-effect)]
                              (if (and (= owner-id-play-effect owner-id-summoned) (not (= minion-play-effect-id minion-summoned-id)))
                                (damage-random state 1 enemy-id)
                                state)))
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
    :states            [:effect]
    :states-cast-spell (fn [state rest]
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
                   (deal-damages state (get-opposing-player-id state) 3 {}))}

   "Ragnaros the Firelord"
   {:attack          8
    :description     "Can't attack. At the end of your turn, deal 8 damage to a random enemy."
    :health          8
    :mana-cost       8
    :name            "Ragnaros the Firelord"
    :rarity          :legendary
    :set             :hall-of-fame
    :type            :minion
    :states          [:cant-attack :effect]
    :states-end-turn (fn [state other-args]
                       (let [minion-play-effect (:minion-play-effect other-args)
                             minion-owner-id (:owner-id minion-play-effect)
                             enemy-id (get-opposing-player-id state minion-owner-id)]
                         (if (= minion-owner-id (get-player-id-in-turn state))
                           (damage-random state 8 enemy-id)
                           state)))}

   "Shield Slam"
   {:class        :warrior
    :description  "Deal 1 damage to a minion for each Armor you have."
    :mana-cost    1
    :name         "Shield Slam"
    :rarity       :epic
    :set          :classic
    :type         :spell
    :states-spell (fn [state other-args]
                    (let [card (:spell-played other-args)
                          target-minion-id (:target-id other-args)
                          owner-id (get-in card [:owner-id])
                          number-armor (get-armor state (get-hero-id-from-player-id state owner-id))]
                      (deal-damages state target-minion-id number-armor {})))
    :valid-target (fn [state card]
                    (vec (map :id (get-minions state))))}

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
    :states-spell (fn [state other-args]
                    (let [minions-list (get-minions state)
                          deal-one-damage (fn [s minion] (deal-damages s (:id minion) 1 {}))]
                      (reduce deal-one-damage state minions-list)))
    }

   ;;New Cards Sprint 3
   "Blubber Baron"
   {:attack                       1
    :description                  "Whenever you summon a Battlecry minion while this is in your hand gain +1/+1."
    :health                       1
    :mana-cost                    3
    :name                         "Blubber Baron"
    :rarity                       :epic
    :set                          :mean-streets-of-gadgetzan
    :type                         :minion
    :states-summon-minion-in-hand (fn [state other-args]
                                    (let [minion-summoned (:card-minion-summoned other-args)
                                          card-play-effect (:card-play-effect other-args)
                                          minion-owner-id (:owner-id minion-summoned)
                                          card-owner-id (:owner-id card-play-effect)]
                                      (if (or (= nil (:battlecry (get-definition (:name minion-summoned)))) (not= minion-owner-id card-owner-id))
                                        state
                                        (-> state
                                            (update-card minion-owner-id (:id card-play-effect) :attack inc)
                                            (update-card minion-owner-id (:id card-play-effect) :health inc)))))}

   "Malorne"
   {:description "Deathrattle: Shuffle this minion into your deck."
    :name        "Malorne"
    :type        :minion
    :mana-cost   7
    :class       :druid
    :health      7
    :set         :goblins-vs-gnomes
    :rarity      :legendary
    :attack      9
    :deathrattle (fn [state other-args]
                   (let [minion-play-effect (:minion-play-effect other-args)
                         owner-id (:owner-id minion-play-effect)]
                     (-> state
                         (add-card-to-deck owner-id (:name minion-play-effect))
                         (shuffle-deck owner-id))))}

   "Far Sight"
   {:class        :shaman
    :description  "Draw a card. That card costs (3) less."
    :mana-cost    3
    :name         "Far Sight"
    :rarity       :epic
    :set          :classic
    :type         :spell
    :states-spell (fn [state other-args]
                    (let [player-id-in-turn (get-player-id-in-turn state)
                          card (nth (get-deck state player-id-in-turn) 0)
                          card-id (:id card)
                          card-mana (:mana-cost card)]
                      (as-> state $
                            (draw-card $ player-id-in-turn)
                            (update-card $ player-id-in-turn card-id :mana-cost
                                         (if (< card-mana 3)
                                           0
                                           (- card-mana 3))))))}

   "Abusive Sergeant"
   {:attack       1
    :description  "Battlecry: Give a minion +2 Attack this turn."
    :health       1
    :mana-cost    1
    :name         "Abusive Sergeant"
    :rarity       :common
    :set          :classic
    :type         :minion
    :battlecry    (fn [state other-args]
                    (let [played-card (:played-card other-args)
                          target-minion-id (:target-id other-args)]
                      (if (some? target-minion-id)
                        (update-attack state target-minion-id 2)
                        state)))
    :valid-target (fn [state card]
                    (vec (map :id (get-minions state))))}

   "Maexxna"
   {:attack      2
    :description "Destroy any minion damaged by this minion."
    :health      8
    :mana-cost   6
    :name        "Maexxna"
    :rarity      :legendary
    :set         :curse-of-naxxramas
    :type        :minion
    :states      [:poisonous]}

   "Explosive Trap"
   {:class         :hunter
    :description   "Secret: When your hero is attacked deal 2 damage to all enemies."
    :mana-cost     2
    :name          "Explosive Trap"
    :rarity        :common
    :set           :classic
    :type          :spell
    :states-spell  (fn [state other-args]
                     (let [card (:spell-played other-args)
                           player-id (:owner-id card)]
                       (add-secret state player-id (create-secret (:name card)))))
    :secret-attack (fn [state other-args]
                     (let [card (:secret-played other-args)
                           attacked-character (:attacked-character other-args)
                           secret-owner-id (:owner-id card)
                           attacked-owner-id (or (:owner-id attacked-character) (get-owner-id state (:id attacked-character)))]
                       (if (and (= :hero (:entity-type attacked-character)) (= secret-owner-id attacked-owner-id))
                         (-> (reduce (fn [s minion] (deal-damages s (:id minion) 2 {})) state (get-minions state (get-opposing-player-id state secret-owner-id)))
                             (deal-damages (get-opposing-player-id state secret-owner-id) 2 {}))
                         state)))}

   "Steward of Darkshire"
   {:description          "Whenever you summon a 1-Health minion, give it Divine Shield."
    :name                 "Steward of Darkshire"
    :type                 :minion
    :mana-cost            3
    :class                :paladin
    :health               3
    :set                  :whispers-of-the-old-gods
    :rarity               :rare
    :attack               3
    :states               [:effect]
    :states-summon-minion (fn [state other-args]
                            (let [minion-summoned (:minion-summoned other-args)
                                  minion-summoned-health (get-health minion-summoned)
                                  minion-summoned-owner-id (:player-summon other-args)
                                  minion-play-effect (:minion-play-effect other-args)]
                              (if (and (= 1 minion-summoned-health) (= minion-summoned-owner-id (:owner-id minion-play-effect)))
                                (set-effect state (:id minion-summoned) :divine-shield)
                                state)))}

   "Doomsayer"
   {:attack            0
    :description       "At the start of your turn destroy ALL minions."
    :health            7
    :mana-cost         2
    :name              "Doomsayer"
    :rarity            :epic
    :set               :classic
    :type              :minion
    :states            [:effect]
    :states-start-turn (fn [state other-args]
                         (let [player-id-in-turn (get-player-id-in-turn state)
                               owner-id (:owner-id (:minion-play-effect other-args))]
                           (if (= player-id-in-turn owner-id)
                             (remove-all-minions state)
                             state)))}

   "Nat Pagle"
   {:attack            0
    :description       "At the start of your turn, you have a 50% chance to draw an extra card."
    :health            4
    :mana-cost         2
    :name              "Nat Pagle"
    :rarity            :legendary
    :set               :classic
    :type              :minion
    :states            [:effect]
    :states-start-turn (fn [state other-args]
                         (let [player-id-in-turn (get-player-id-in-turn state)
                               owner-id (:owner-id (:minion-play-effect other-args))]
                           (if (and (= player-id-in-turn owner-id) (= ((get-random-int (:counter state) 2) 1) 0))
                             (draw-card state player-id-in-turn)
                             state)))}


   "Spellbreaker"
   {:attack       4
    :description  "Battlecry: Silence a minion."
    :health       3
    :mana-cost    4
    :name         "Spellbreaker"
    :rarity       :common
    :set          :classic
    :type         :minion
    :battlecry    (fn [state other-args]
                    (let [target-minion-id (:target-id other-args)]
                      (set-effect state target-minion-id :silenced)))
    :valid-target (fn [state card]
                    (vec (map :id (get-minions state))))}

   "Deathwing"
   {:attack      12
    :description "Battlecry: Destroy all other minions and discard your hand."
    :health      12
    :mana-cost   10
    :name        "Deathwing"
    :rarity      :legendary
    :set         :classic
    :type        :minion
    :battlecry   (fn [state other-args]
                   (let [played-card-owner-id (:owner-id (:played-card other-args))]
                     (-> state
                         (remove-all-minions)
                         (remove-all-cards-from-hand played-card-owner-id))))}

   "Sylvanas Windrunner"
   {:attack      5
    :description "Deathrattle: Take control of a random enemy minion." ;
    :health      5
    :mana-cost   6
    :name        "Sylvanas Windrunner"
    :rarity      :legendary
    :set         :hall-of-fame
    :type        :minion
    :deathrattle (fn [state other-args]
                   (let [minion-play-effect (:minion-play-effect other-args)
                         owner-id (:owner-id minion-play-effect)
                         random-enemy-minion (get-random-minion state (get-opposing-player-id state owner-id))]
                     (if (= random-enemy-minion nil)
                       state
                       (swap-minion-of-player state (:id random-enemy-minion)))))}

   "Noble Sacrifice"
   {:class         :paladin
    :description   "Secret: When an enemy attacks summon a 2/1 Defender as the new target."
    :mana-cost     1
    :name          "Noble Sacrifice"
    :rarity        :common
    :set           :classic
    :type          :spell
    :states-spell  (fn [state other-args]
                     (let [card (:spell-played other-args)
                           player-id (:owner-id card)]
                       (add-secret state player-id (create-secret (:name card)))))
    :secret-attack (fn [state other-args]
                     (let [card (:secret-played other-args)
                           attacked-character (:attacked-character other-args)
                           attacked-character-id (:id attacked-character)
                           secret-owner-id (:owner-id card)
                           attacked-owner-id (or (:owner-id attacked-character) (get-owner-id state (:id attacked-character)))]
                       (if-not (= secret-owner-id attacked-owner-id)
                         state
                         (-> state
                             (update-id-character attacked-character-id)
                             (summon-minion secret-owner-id (create-card "Defender" :id attacked-character-id) (inc (count (get-minions state secret-owner-id))))))))}

   "Mountain Giant"
   {:attack      8
    :description "Costs (1) less for each other card in your hand."
    :health      8
    :mana-cost   12
    :name        "Mountain Giant"
    :rarity      :epic
    :set         :classic
    :type        :minion}

   "Mana Wraith"
   {:attack      2
    :description "ALL minions cost (1) more."
    :health      2
    :mana-cost   2
    :name        "Mana Wraith"
    :rarity      :rare
    :set         :classic
    :type        :minion}

   "Lowly Squire"
   {:attack      1
    :description "Inspire: Gain +1 Attack."
    :health      2
    :mana-cost   1
    :name        "Lowly Squire"
    :rarity      :common
    :set         :the-grand-tournament
    :type        :minion
    :states      [:inspire]
    :inspire     (fn [state other-args]
                   (let [minion-play-effect (:minion-play-effect other-args)
                         power-owner-id (:power-owner-id other-args)]
                     (if (= power-owner-id (:owner-id minion-play-effect))
                       (update-minion state (:id minion-play-effect) :attack inc)
                       state)))}

   "Sunwalker"
   {:attack      4
    :description "Taunt. Divine Shield."
    :health      5
    :mana-cost   6
    :name        "Sunwalker"
    :rarity      :rare
    :set         :classic
    :type        :minion
    :states      [:taunt :divine-shield]}

   "Loot Hoarder"
   {:attack      2
    :description "Deathrattle: Draw a card."
    :health      1
    :mana-cost   2
    :name        "Loot Hoarder"
    :rarity      :common
    :set         :classic
    :type        :minion
    :deathrattle (fn [state other-args]
                   (let [minion-play-effect (:minion-play-effect other-args)
                         owner-id (:owner-id minion-play-effect)]
                     (draw-card state owner-id)))}

   })
(add-definitions! card-definitions)