(ns firestone.client.kth.endpoints
  (:require [clojure.pprint :refer [pprint]]
            [firestone.client.kth.edn-api :refer [create-game!
                                                  play-minion-card!
                                                  end-turn!
                                                  attack!
                                                  use-spell!]]))

(defn create-response
  [game]
  {:status  200
   :headers {"Content-Type"                 "application/edn; charset=utf-8"
             "Access-Control-Allow-Origin"  "*"
             "Access-Control-Allow-Methods" "*"}
   :body    (str game)})

(defn handler!
  [request]
  ;(pprint request)
  (let [uri (:uri request)
        ; when body contains params we will extract them
        params (when-let [body-as-stream (:body request)]
                 (-> body-as-stream
                     (slurp)
                     (read-string)))]
    (println uri)
    (cond (= uri "/create-game")
          (create-response(create-game!))

          (= uri "/play-minion-card")
          (let [position (:position params)
                card-id (:card-id params)
                player-id (:player-id params)]
            (create-response (play-minion-card! player-id card-id position)))
          (= uri "/end-turn")
          (let [player-id (:player-id params)]
          (create-response (end-turn! player-id)))

          (= uri "/attack")
          (let [player-id (:player-id params)
                attacker-card-id (:attacker-id params)
                defender-card-id (:target-id params)]
            (create-response (attack! player-id attacker-card-id defender-card-id)))

          (= uri "/play-spell-card")
          (let [player-id (:player-id params)
                card-id (:card-id params)
                target-id (:target-id params)]
            (create-response (use-spell! player-id card-id target-id)))

          (= uri "/engine-settings")
          {:status  200
           :headers {"Content-Type"                 "application/edn; charset=utf-8"
                     "Access-Control-Allow-Origin"  "*"
                     "Access-Control-Allow-Methods" "*"}
           :body    (str {:supports-undo false
                          :supports-redo false
                          :audio         :auto})}

          :else
          {:status  404
           :headers {"Content-Type" "text/html"}
           :body    "<h1>Missing endpoint!</h1>"})

    ))
