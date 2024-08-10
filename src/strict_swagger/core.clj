(ns strict-swagger.core
  (:require [cheshire.generate :refer [add-encoder]]
            [ring.swagger.json-schema :refer [JsonSchema]]
            [strict.core :as st]))

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
(defmethod type-of-validator (:name st/string) [_] {:type "string"})
(defmethod type-of-validator (:name st/string-like) [_] {:type "string"})
(defmethod type-of-validator (:name st/number-str) [_] {:type "number"})
(defmethod type-of-validator (:name st/number) [_] {:type "number"})
(defmethod type-of-validator (:name st/boolean-str) [_] {:type "boolean"})
(defmethod type-of-validator (:name st/boolean) [_] {:type "boolean"})
(defmethod type-of-validator (:name st/integer-str) [_] {:type "integer"})
(defmethod type-of-validator (:name st/integer) [_] {:type "integer"})
(defmethod type-of-validator (:name st/uuid-str) [_] {:type "string" :format "uuid"})
(defmethod type-of-validator (:name st/member) [maybe-validator] {:type "string" :enum (vec (second maybe-validator))})
(defmethod type-of-validator (:name st/map) [_] {:type "object"})


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
    {:type "array"
     :items nested-spec}))

(defmethod type-of-validator (:name st/nested)
  [maybe-validator]
  (let [nested-map     (cond
                         (st/validator? maybe-validator)
                         (throw (ex-info "the parameter of nested validator must be a map" {:nested-validator maybe-validator}))

                         (map? maybe-validator)
                         maybe-validator

                         :else
                         (second maybe-validator))
        nested-spec    (validator-map->swagger-parameter-spec nested-map st/nested)
        required-props (->> nested-map
                            (filter (fn [[k v]] (contains-required-validator? v)))
                            (map first)
                            vec)
        additional-props? (::additional-properties? (meta nested-map))]
    (cond-> {:type "object"
             :properties nested-spec}
      (seq required-props)
      (assoc :required required-props)
      
      additional-props?
      (assoc :additionalProperties true))))


(defmulti sample-json validator-name)

(defmethod sample-json :default [_] nil)
(defmethod sample-json (:name st/string) [_] "string")
(defmethod sample-json (:name st/string-like) [_] "string")
(defmethod sample-json (:name st/number-str) [_] "1.0")
(defmethod sample-json (:name st/number) [_] 1.0)
(defmethod sample-json (:name st/boolean-str) [_] "true")
(defmethod sample-json (:name st/boolean) [_] true)
(defmethod sample-json (:name st/integer-str) [_] "1")
(defmethod sample-json (:name st/integer) [_] 1)
(defmethod sample-json (:name st/uuid-str) [_] (str (random-uuid)))
(defmethod sample-json (:name st/member) [maybe-validator] (let [items (second maybe-validator)] (first items)))
(defmethod sample-json (:name st/map) [_] {"any" "item"})

(defmethod sample-json (:name st/coll-of)
  [maybe-validator]
  (let [nested-items (second maybe-validator)]
    (mapv sample-json nested-items)))

(defmethod sample-json (:name st/nested)
  [maybe-validator]
  (let [nested-map  (cond
                      (st/validator? maybe-validator)
                      (throw (ex-info "the parameter of nested validator must be a map" {:nested-validator maybe-validator}))

                      (map? maybe-validator)
                      maybe-validator

                      :else
                      (second maybe-validator))]
    (into {} (map (fn [[k v]] [k (sample-json v)]) nested-map))))

(deftype FieldValidatorJsonSchema [validator-vec]
  JsonSchema
  (convert [this option]
    (validator-vec->swagger-parameter-spec validator-vec)) 
  
  Object
  (equals [this right] (= (.-validator-vec this) (.-validator-vec right)))
  (hashCode [this] (hash (.-validator-vec this)))
  (toString [this] (str "FieldValidatorJsonSchema(validator-vec = " (.-validator-vec this) ")")))

(defn validator
  [^FieldValidatorJsonSchema schema]
  (.-validator-vec schema))

(defn validator-json-schema [validator-vec] (FieldValidatorJsonSchema. validator-vec))

(add-encoder FieldValidatorJsonSchema
             (fn [item json-generator]
               (let [validator-vec-vec (.-validator-vec item)]
                 (.writeString json-generator (sample-json validator-vec-vec)))))

(defn validator-vec->swagger-parameter-spec
  [validator-vec & [parent-validator]] 
  (letfn [(resolve-type
           [spec-vec]
           (let [reversed (reverse spec-vec)]
             (some type-of-validator reversed)))]
    
    (let [validator-vec (if (st/validator? validator-vec) [validator-vec] validator-vec)
          type-map  (resolve-type validator-vec)
          required? (contains-required-validator? validator-vec)]
      (cond-> {}
        (and (not (= (:name st/nested) (:name parent-validator)))
             required?)
        (assoc :required true)

        (some? type-map)
        (merge type-map)))))

(defn validator-map->swagger-parameter-spec
  [param-validators-map & [parent-validator]]
  (into {}
        (map (fn [[param-name validators]]
               [param-name (validator-vec->swagger-parameter-spec validators parent-validator)]))
        param-validators-map))

(defn json-schema
  [param-validators-map]
  (validator-json-schema [[st/nested param-validators-map]]))
