(ns reaver-test
  (:require [clojure.test :as test :refer [deftest is testing]]
            [reaver])
  (:import [org.jsoup.nodes Document Element]))

(def basic-html-doc
  "<html>
    <head></head>
    <body>
      <div id=\"one\" class=\"first\">
       <p>Hello...</p>
      </div>
      <div id=\"two\" class=\"second\">
        <p id=\"three\">...World.</p>
        <p id=\"four\">...Sailor.</p>
        <p id=\"five\">...Darkness my old friend.</p>
      </div>
    </body>
  </html>")

(def basic-html-fragment
  "<p>Hello, World!</p>")

(deftest parse-test
  (testing "can parse an HTML document from a string"
    (let [parsed (reaver/parse basic-html-doc)]
      (is (instance? Document parsed)))))

(deftest parse-fragment-test
  (testing "can parse an HTML fragment from a string"
    (let [parsed (reaver/parse-fragment basic-html-fragment)]
      (is (instance? Element parsed)))))

(deftest extract-test
  (testing "can extract on a HTML document, returning a seq of matches"
    (let [parsed (reaver/parse basic-html-doc)
          matches (reaver/extract parsed
                                  []
                                  ".second p" reaver/text)]
      (is (= (count matches) 3)))))

(deftest extract-from-test
  (testing "can extract-from on a HTML document, returning a seq of matches"
    (let [parsed (reaver/parse basic-html-doc)
          matches (reaver/extract-from parsed "div"
                                       [:p-text]
                                       "p" reaver/text)]
      (is (= (count matches) 2)))))
