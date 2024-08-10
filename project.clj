(defproject org.clojars.t_yano/strict-swagger "0.1.0-SNAPSHOT"
  :description "The library to generate a map-data for ring-swagger from the validation spec of 'strict' library."
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[cheshire "5.13.0"]]
  :profiles 
  {:provided 
   {:dependencies [[org.clojars.t_yano/strict "2.0.0"]
                   [org.clojure/clojure "1.11.1"]
                   [metosin/ring-swagger "0.22.3"]
                   [prismatic/schema "1.4.1"]]}
   :dev
   {:dependencies [[nubank/matcher-combinators "3.9.1"]]}}
  :repl-options {:init-ns strict-swagger.core})
