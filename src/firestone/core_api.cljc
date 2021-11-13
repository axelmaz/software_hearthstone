(ns firestone.core-api
  (:require [ysera.test :refer [is is= error?]]
            [ysera.error :refer [error]]
            [firestone.construct :refer [add-card-to-deck
                                         add-card-to-hand
                                         create-card
                                         create-game
                                         draw-card
                                         get-card-from-hand
                                         get-deck
                                         get-hand
                                         get-player-id-in-turn
                                         get-players
                                         remove-card-from-hand]]))

(defn end-turn
  {:test (fn []
           (is= (-> (create-game)
                    (end-turn "p1")
                    (get-player-id-in-turn))
                "p2")
           (is= (-> (create-game)
                    (end-turn "p1")
                    (end-turn "p2")
                    (get-player-id-in-turn))
                "p1")
           (error? (-> (create-game)
                       (end-turn "p2"))))}
  [state player-id]
  (when-not (= (get-player-id-in-turn state) player-id)
    (error "The player with id " player-id " is not in turn."))
  (let [player-change-fn {"p1" "p2"
                          "p2" "p1"}]
    (-> state
        (update :player-id-in-turn player-change-fn))))


(defn play-minion-card
  {:test (fn []
           ; Shouldn't be able to play a card when not in turn
           (error? (-> (create-game [{:hand [(create-card "Defender" :id "d")]}]
                                    :player-id-in-turn "p2")
                       (play-minion-card "p1" "d" 0)))
           ; Shouldn't not be able to play cards not in our hand
           (error? (-> (create-game)
                       (play-minion-card "p1" "n" 0)))
           ; Shouldn't be able to play a card if not enough mana
           ;(error? (-> (create-game [{:hand [(create-card "Nightblade" :id "n")]
           ;                           :mana 3}])
           ;            (play-minion-card "p1" "n" 0)))
           ; The card should be removed from hand
           (is (-> (create-game [{:hand [(create-card "Defender" :id "d")]}])
                   (play-minion-card "p1" "d" 0)
                   (get-hand "p1")
                   (empty?))))}
  [state player-id card-id position]
  (when-not (= (get-player-id-in-turn state) player-id)
    (error "The player with id " player-id " is not in turn."))
  (let [card (get-card-from-hand state player-id card-id)]
    (when-not card
      (error "The card with id " card-id " is not in the hand of the given player."))
    ;(when-not (enough-mana? state player-id card)
    ;  (error "Not enough mana."))
    (-> state
        (remove-card-from-hand player-id card-id))))

;Test area
(def m (create-game[{:deck ["Novice Engineer" "Defender"]}]))
(def b (add-card-to-deck m "p2" "Nightblade"))
(def n (add-card-to-hand b "p1" "Nightblade"))

(get-player-id-in-turn n)
(get-players n)
(def v (draw-card n "p1"))
(get-in (get-hand v "p1") [1 :name])
(get-card-from-hand v "p1" "c1")
((nth (get-deck v "p1") 0) :id)
(not (get-player-id-in-turn n))
(end-turn n "p1")

(def m2 (create-game))
(empty? (get-deck m2 "p1"))
(get-in m2 [:players "p1" :hero :damage-taken])
(draw-card m2 "p1")