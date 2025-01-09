(defproject reaver "0.1.3"
  :description "Extract data from HTML with Clojure."
  :url "https://github.com/mischov/reaver"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.jsoup/jsoup "1.18.3"]]
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.12.0"]
                                  [criterium "0.4.6"]]
                   :warn-on-reflection true}})
