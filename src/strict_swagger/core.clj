(ns strict-swagger.core
  (:require [schema.core :as s]
            [strict.core :as st]
            [ring.swagger.json-schema :as swagger]))

(defn- unwrap-validator [validator] (if (vector? validator) (first validator) validator))

(defn- validator-name
  [maybe-validator]
  (cond
    (st/validator? maybe-validator)
    (:name maybe-validator)
  
    (map? maybe-validator)
    (:name st/nested)
  
    :else
    (let [validator (unwrap-validator maybe-validator)]
      (:name validator))))

(defn- contains-required-validator?
 [spec-vec]
 (some (fn [validator] (let [validator (unwrap-validator validator)]
                         (= (:name st/required) (:name validator))))
       spec-vec))

(defmulti type-of-validator validator-name)

(defmethod type-of-validator :default [_] nil)
(defmethod type-of-validator (:name st/string) [_] s/Str)
(defmethod type-of-validator (:name st/string-like) [_] s/Str)
(defmethod type-of-validator (:name st/number-str) [_] s/Num)
(defmethod type-of-validator (:name st/number) [_] s/Num)
(defmethod type-of-validator (:name st/boolean-str) [_] s/Bool)
(defmethod type-of-validator (:name st/boolean) [_] s/Bool)
(defmethod type-of-validator (:name st/integer-str) [_] s/Int)
(defmethod type-of-validator (:name st/integer) [_] s/Int)
(defmethod type-of-validator (:name st/uuid-str) [_] s/Uuid)
(defmethod type-of-validator (:name st/uuid) [_] s/Uuid)
(defmethod type-of-validator (:name st/member) [maybe-validator] (apply s/enum (second maybe-validator)))
(defmethod type-of-validator (:name st/map) [_] {s/Any s/Any})


(declare validator-map->swagger-parameter-spec validator-vec->swagger-parameter-spec)

(defmethod type-of-validator (:name st/coll-of)
  [maybe-validator]
  (let [nested-type (second maybe-validator)
        nested-type (cond
                      (st/validator? nested-type)
                      [nested-type]

                      (map? nested-type)
                      [[st/nested nested-type]]

                      :else
                      nested-type)
        nested-spec (validator-vec->swagger-parameter-spec nested-type)]
    [nested-spec]))

(defmethod type-of-validator (:name st/nested)
  [maybe-validator]
  (let [nested-map (cond
                     (st/validator? maybe-validator)
                     (throw (ex-info "the parameter of nested validator must be a map" {:nested-validator maybe-validator}))

                     (map? maybe-validator)
                     maybe-validator

                     :else
                     (second maybe-validator))

        required-props (->> nested-map
                            (filter (fn [[k v]] (contains-required-validator? v)))
                            (map first)
                            set)
        
        metadata (meta nested-map)]
    (cond->
     (into {}
           (map (fn [[k v]] [(if (required-props k)
                               (s/required-key k)
                               (s/optional-key k))
                             (validator-vec->swagger-parameter-spec v)]))
           nested-map)
      (some? metadata)
      (with-meta metadata))))

(defn apply-meta
  [schema metadata]
  (swagger/field schema metadata))

(defn validator-vec->swagger-parameter-spec
  [validator-vec] 
  (letfn [(resolve-type
           [spec-vec metadata] 
           (let [reversed (reverse spec-vec)
                 resolved (some type-of-validator reversed)]
             (when resolved
               (if (some? metadata)
                 (apply-meta resolved metadata)
                 resolved))))]
    
    (let [metadata (meta validator-vec)
          validator-vec (cond
                          (st/validator? validator-vec)
                          [validator-vec]
                          
                          (map? validator-vec)
                          [st/nested validator-vec]

                          :else
                          validator-vec)]
      (resolve-type validator-vec metadata))))

(defn validator-map->swagger-parameter-spec
  [param-validators-map]
  (let [metadata (meta param-validators-map)]
    (cond-> (into {}
                  (map (fn [[param-name validators]]
                         (let [required? (contains-required-validator? validators)]
                           [(if required?
                              (s/required-key param-name)
                              (s/optional-key param-name))
                            (validator-vec->swagger-parameter-spec validators)])))
                  param-validators-map)
      (some? metadata)
      (with-meta metadata))))

(def json-schema validator-map->swagger-parameter-spec)
