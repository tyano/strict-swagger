(ns strict-swagger.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [matcher-combinators.test :refer [match?]]
            [schema.core :as s]
            [strict-swagger.core :refer [validator-map->swagger-parameter-spec
                                         validator-vec->swagger-parameter-spec] :as core]
            [strict.core :as st]))

(deftest validator-vec->swagger-parameter-spec-test
  (testing "Simple conversion test"
    (is (= s/Str (validator-vec->swagger-parameter-spec [st/string])))
    (is (= s/Str (validator-vec->swagger-parameter-spec [st/string-like])))
    (is (= s/Num (validator-vec->swagger-parameter-spec [st/number-str])))
    (is (= s/Num (validator-vec->swagger-parameter-spec [st/number])))
    (is (= s/Bool (validator-vec->swagger-parameter-spec [st/boolean-str])))
    (is (= s/Bool (validator-vec->swagger-parameter-spec [st/boolean])))
    (is (= s/Int (validator-vec->swagger-parameter-spec [st/integer-str])))
    (is (= s/Int (validator-vec->swagger-parameter-spec [st/integer])))
    (is (= s/Uuid (validator-vec->swagger-parameter-spec [st/uuid-str])))
    (is (= s/Uuid (validator-vec->swagger-parameter-spec [st/uuid])))
    (is (= {s/Any s/Any} (validator-vec->swagger-parameter-spec [st/map])))

    (is (= (s/enum "a" "b" "c")
           (validator-vec->swagger-parameter-spec [[st/member ["a" "b" "c"]]])))

    (is (= [s/Str]
           (validator-vec->swagger-parameter-spec [[st/coll-of st/string]])))

    (is (= [{(s/optional-key :a) s/Str}]
           (validator-vec->swagger-parameter-spec [[st/coll-of {:a [st/string]}]])))

    (is (= [{(s/optional-key :a) s/Str}]
           (validator-vec->swagger-parameter-spec [[st/coll-of [st/nested {:a [st/string]}]]])))

    (is (= (s/enum "apple" "orange")
           (validator-vec->swagger-parameter-spec [[st/member ["apple" "orange"]]])))

    (is (= {(s/optional-key :a) s/Str}
           (validator-vec->swagger-parameter-spec [[st/nested {:a [st/string]}]])))

    (is (= {(s/optional-key :a) s/Str}
           (validator-vec->swagger-parameter-spec [{:a [st/string]}])))

    (is (= {(s/optional-key :a) s/Str}
           (validator-vec->swagger-parameter-spec [{:a st/string}])))
    
    (is (= {(s/optional-key :a) s/Str}
           (validator-vec->swagger-parameter-spec [^{::core/name "Username"} {:a st/string}])))
    
    (is (= {(s/required-key :a) s/Str}
           (validator-vec->swagger-parameter-spec [{:a [st/required st/string]}]))))
  
  #_(testing "if some map have 'additional-properties true' metadata, add 'additionalProperties true' to the swagger spec"
    (is (= {:type "object" :properties {:a {:type "string"}} :additionalProperties true}
           (validator-vec->swagger-parameter-spec [^::core/additional-properties? {:a st/string}]))))

  (testing "each validator can be wrapper by vector if the validator doen't have any parameters"
    (is (= s/Str (validator-vec->swagger-parameter-spec [[st/string]])))
    (is (= s/Str (validator-vec->swagger-parameter-spec [[st/string-like]])))
    (is (= s/Num (validator-vec->swagger-parameter-spec [[st/number-str]])))
    (is (= s/Num (validator-vec->swagger-parameter-spec [[st/number]])))
    (is (= s/Bool (validator-vec->swagger-parameter-spec [[st/boolean-str]])))
    (is (= s/Bool (validator-vec->swagger-parameter-spec [[st/boolean]])))
    (is (= s/Int (validator-vec->swagger-parameter-spec [[st/integer-str]])))
    (is (= s/Int (validator-vec->swagger-parameter-spec [[st/integer]])))
    (is (= s/Uuid (validator-vec->swagger-parameter-spec [[st/uuid-str]])))
    (is (= {s/Any s/Any} (validator-vec->swagger-parameter-spec [[st/map]])))))

(deftest description-test
  (testing "If validation spec is annotated with swagger/field or swagger/description, generated schema must have the data as it's metadata"
    (let [converted (validator-map->swagger-parameter-spec 
                     {:testkey ^{:description "This is a test"} [st/required st/string]})]
      (is (= "This is a test"
             (-> converted (get :testkey) meta :json-schema :description))))))

(deftest validator-map->swagger-parameter-spec-test
  (testing "complicated case"
    (is (= {(s/required-key :accountId) s/Str,
            (s/optional-key :attributes) {s/Any s/Any},
            (s/required-key :idInfo)
            [{(s/required-key :service) (s/enum "apple" "orange"),
              (s/required-key :userId) s/Str}]}

           (validator-map->swagger-parameter-spec
            {:accountId [st/required st/string]
             :attributes [st/map]
             :idInfo [st/required
                      [st/coll-of [^{::core/name "SearchParam"} {:service [st/required [st/member ["apple" "orange"] :coerce keyword :message "invalid service type"]]
                                                                 :userId [st/required st/string]}]]]})))))

(deftest keep-map-metadata
  (testing "keep metadata of the map"
    (let [converted (validator-map->swagger-parameter-spec 
                     ^{:sample "sample"} {:a [st/required st/string], 
                                          :b ^{:submap "submap"} {:c [st/integer]}})]
      (is (= {:sample "sample"}
             (meta converted)))
      (is (match? {:submap "submap"}
                  (meta (get converted (s/optional-key :b))))))))