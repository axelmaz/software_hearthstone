(ns firestone.definition.hero
  (:require [firestone.definitions :refer [add-definitions!]]))

(def hero-definitions
  {

   "Jaina Proudmoore"
   {:name       "Jaina Proudmoore"
    :class      :mage
    :type       :hero
    :health     30
    :hero-power "Fireblast"}

   "Thrall"
   {:name       "Thrall"
    :type       :hero
    :class      :shaman
    :health     30
    :hero-power "Totemic Call"}

   })

(add-definitions! hero-definitions)