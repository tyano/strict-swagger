(ns strict-swagger.core-test
  (:require [clojure.test :refer [is deftest testing]]
            [strict-swagger.core :refer [validator-vec->swagger-parameter-spec validator-map->swagger-parameter-spec]]
            [strict.core :as st]))

(deftest validator-vec->swagger-parameter-spec-test
  (testing "Simple conversion test"
    (is (= {:type "string"} (validator-vec->swagger-parameter-spec [st/string])))
    (is (= {:type "string"} (validator-vec->swagger-parameter-spec [st/string-like])))
    (is (= {:type "number"} (validator-vec->swagger-parameter-spec [st/number-str])))
    (is (= {:type "number"} (validator-vec->swagger-parameter-spec [st/number])))
    (is (= {:type "boolean"} (validator-vec->swagger-parameter-spec [st/boolean-str])))
    (is (= {:type "boolean"} (validator-vec->swagger-parameter-spec [st/boolean])))
    (is (= {:type "integer"} (validator-vec->swagger-parameter-spec [st/integer-str])))
    (is (= {:type "integer"} (validator-vec->swagger-parameter-spec [st/integer])))
    (is (= {:type "string" :format "uuid"} (validator-vec->swagger-parameter-spec [st/uuid-str])))
    (is (= {:type "object"} (validator-vec->swagger-parameter-spec [st/map])))

    (is (= {:type "string" :enum ["a" "b" "c"]}
           (validator-vec->swagger-parameter-spec [[st/member ["a" "b" "c"]]])))

    (is (= {:type "array" :items {:type "string"}}
           (validator-vec->swagger-parameter-spec [[st/coll-of st/string]])))

    (is (= {:type "array" :items {:type "object" :properties {:a {:type "string"}}}}
           (validator-vec->swagger-parameter-spec [[st/coll-of {:a [st/string]}]])))

    (is (= {:type "array" :items {:type "object" :properties {:a {:type "string"}}}}
           (validator-vec->swagger-parameter-spec [[st/coll-of [st/nested {:a [st/string]}]]])))

    (is (= {:type "string" :enum ["apple" "orange"]}
           (validator-vec->swagger-parameter-spec [[st/member ["apple" "orange"]]])))

    (is (= {:type "object" :properties {:a {:type "string"}}}
           (validator-vec->swagger-parameter-spec [[st/nested {:a [st/string]}]])))

    (is (= {:type "object" :properties {:a {:type "string"}}}
           (validator-vec->swagger-parameter-spec [{:a [st/string]}])))

    (is (= {:type "object" :properties {:a {:type "string"}}}
           (validator-vec->swagger-parameter-spec [{:a st/string}])))
    
    (is (= {:type "object" :properties {:a {:type "string"}} :required [:a]}
           (validator-vec->swagger-parameter-spec [{:a [st/required st/string]}]))))

  (testing "each validator can be wrapper by vector if the validator doen't have any parameters"
    (is (= {:type "string"} (validator-vec->swagger-parameter-spec [[st/string]])))
    (is (= {:type "string"} (validator-vec->swagger-parameter-spec [[st/string-like]])))
    (is (= {:type "number"} (validator-vec->swagger-parameter-spec [[st/number-str]])))
    (is (= {:type "number"} (validator-vec->swagger-parameter-spec [[st/number]])))
    (is (= {:type "boolean"} (validator-vec->swagger-parameter-spec [[st/boolean-str]])))
    (is (= {:type "boolean"} (validator-vec->swagger-parameter-spec [[st/boolean]])))
    (is (= {:type "integer"} (validator-vec->swagger-parameter-spec [[st/integer-str]])))
    (is (= {:type "integer"} (validator-vec->swagger-parameter-spec [[st/integer]])))
    (is (= {:type "string" :format "uuid"} (validator-vec->swagger-parameter-spec [[st/uuid-str]])))
    (is (= {:type "object"} (validator-vec->swagger-parameter-spec [[st/map]]))))

  (testing "':required true' must be added if the validator-vec contains a strict/required validator"
    (is (= {:type "string", :required true} (validator-vec->swagger-parameter-spec [st/required st/string])))))

(deftest validator-map->swagger-parameter-spec-test
  (testing "complicated case"
    (is (= {:accountId {:required true, :type "string"},
            :attributes {:type "object"},
            :idInfo
            {:required true,
             :type "array",
             :items
             {:type "object",
              :properties
              {:service
               {:type "string",
                :enum ["apple" "orange"]},
               :userId {:type "string"}}
             
              :required [:service :userId]}}}

           (validator-map->swagger-parameter-spec
            {:accountId [st/required
                         st/string]
             :attributes [st/map]
             :idInfo [st/required
                      [st/coll-of {:service [st/required [st/member ["apple" "orange"] :coerce keyword :message "invalid service type"]]
                                   :userId [st/required st/string]}]]})))))
