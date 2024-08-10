(ns strict-swagger.core-test
  (:require [clojure.test :refer [is deftest testing]]
            [strict-swagger.core :refer [validator-vec->swagger-parameter-spec validator-map->swagger-parameter-spec] :as core]
            [strict.core :as st]
            [schema.core :as s]))

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
    (is (= {s/Any s/Any} (validator-vec->swagger-parameter-spec [st/map])))

    (is (= (s/enum "a" "b" "c")
           (validator-vec->swagger-parameter-spec [[st/member ["a" "b" "c"]]])))

    (is (= [s/Str]
           (validator-vec->swagger-parameter-spec [[st/coll-of st/string]])))

    (is (= [{:a s/Str}]
           (validator-vec->swagger-parameter-spec [[st/coll-of {:a [st/string]}]])))

    (is (= [{:a s/Str}]
           (validator-vec->swagger-parameter-spec [[st/coll-of [st/nested {:a [st/string]}]]])))

    (is (= (s/enum "apple" "orange")
           (validator-vec->swagger-parameter-spec [[st/member ["apple" "orange"]]])))

    (is (= {:a s/Str}
           (validator-vec->swagger-parameter-spec [[st/nested {:a [st/string]}]])))

    (is (= {:a s/Str}
           (validator-vec->swagger-parameter-spec [{:a [st/string]}])))

    (is (= {:a s/Str}
           (validator-vec->swagger-parameter-spec [{:a st/string}])))
    
    (is (= {:a s/Str}
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
    (is (= {s/Any s/Any} (validator-vec->swagger-parameter-spec [[st/map]]))))

  #_(testing "':required true' must be added if the validator-vec contains a strict/required validator"
    (is (= {:type "string", :required true} (validator-vec->swagger-parameter-spec [st/required st/string])))))

(deftest validator-map->swagger-parameter-spec-test
  (testing "complicated case"
    (is (= {(s/required-key :accountId) s/Str,
            :attributes {s/Any s/Any},
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
      (prn (:b converted))
      (is (= {:submap "submap"}
             (meta (:b converted)))))))