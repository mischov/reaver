(defproject reaver "0.1.1"
  :description "Extract data from HTML with Clojure."
  :url "https://github.com/mischov/reaver"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.jsoup/jsoup "1.8.1"]]
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.6.0"]
                                  [criterium "0.4.3"]]
                   :warn-on-reflection true}})
