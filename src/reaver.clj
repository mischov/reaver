(ns reaver
  (:require [clojure.string :as string])
  (:import [org.jsoup Jsoup]
           [org.jsoup.parser Parser Tag]
           [org.jsoup.nodes Attribute Attributes Comment DataNode
                            Document DocumentType Element Node
                            TextNode]
           [org.jsoup.select Elements]))

;;
;; Parse HTML
;;

(defn parse
  "Parses a string representing a full HTML document
   into Jsoup."
  [^String html]
  (when html
    (Jsoup/parse html)))

(defn parse-fragment
  "Parses a string representing a fragment of HTML
   into Jsoup."
  [^String html]
  (when html
    (.. (Jsoup/parseBodyFragment html) (body) (childNode 0))))

;;
;; Translate Jsoup to EDN
;;

(defn ^:private to-keyword
  "Converts a string into a lowercase keyword."
  [s]
  (-> s string/lower-case keyword))

(defn reduce-into
  "Imperfectly mimics 'into' with 'reduce' and 'conj'
   for better performance."
  [empty-coll xs]
  (reduce conj empty-coll xs))

(defprotocol EDNable
  (to-edn [jsoup]
    "Converts Jsoup into an edn representation of HTML.

       {:type    Keyword
        :tag     Keyword|nil
        :attrs   {Keyword String, ...}|nil
        :content Vector|Map|String|nil}"))

(extend-protocol EDNable
  Attribute
  (to-edn [attr] [(to-keyword (.getKey attr))
                  (.getValue attr)])

  Attributes
  (to-edn [attrs] (not-empty (reduce-into {} (map to-edn attrs))))

  Comment
  (to-edn [comment] {:type :comment
                     :content [(.getData comment)]})

  DataNode
  (to-edn [node] (str node))

  Document
  (to-edn [doc] {:type :document
                 :content (not-empty
                           (reduce-into [] (map to-edn (.childNodes doc))))})

  DocumentType
  (to-edn [doctype] {:type :document-type
                     :attrs (to-edn (.attributes doctype))})

  Element
  (to-edn [element] {:type :element
                     :attrs (to-edn (.attributes element))
                     :tag (to-keyword (.tagName element))
                     :content (not-empty
                               (reduce-into [] (map to-edn (.childNodes element))))})

  Elements
  (to-edn [elements] (when (> (.size elements) 0)
                       (map to-edn elements)))

  TextNode
  (to-edn [node] (.getWholeText node)))

;;
;; Select Elements
;;

(defn select
  "Given HTML parsed to Jsoup and a string representing
   a CSS-esque selector, select* returns Jsoup representing
   any successfully selected data.

   For more on selector syntax, see:
   http://jsoup.org/cookbook/extracting-data/selector-syntax"
  [^Node node ^String css-selector]
  (let [^Elements result (.select node css-selector)]
    (if (.isEmpty result)
      nil
      result)))

;;
;; Extract Data
;;

(defprotocol Extractable
  (jsoup [x]
    "Returns data in Jsoup types.")
  (edn [x]
    "Returns data in edn format.")
  (tag [x]
    "Returns a keyword representing the node's html tag (if it has one)")
  (attr* [x a]
    "Returns a string representing the node's value for the
     supplied attribute (or nil, if it has no value)")
  (attrs [x]
    "Returns a map of keyword/string pairs representing the node's
     attributes.")
  (text [x]
    "Returns a string representing all the text contained by the node.")
  (data [x]
    "Returns a string representing all the data (ie. from scripts)
     contained by the node."))

(extend-protocol Extractable
  nil
  (jsoup
    [_]
    nil)

  (edn
    [_]
    nil)

  (tag
    [_]
    nil)

  (attr*
    [_ _]
    nil)

  (attrs
    [_]
    nil)

  (text
    [_]
    nil)

  (data
    [_]
    nil))

(extend-protocol Extractable
  Element
  (jsoup
    [element]
    element)

  (edn
    [element]
    (to-edn element))

  (tag
    [element]
    (-> (to-edn element)
        :tag))

  (attr*
    [element attribute]
    (-> (to-edn element)
        (get-in [:attrs (keyword attribute)])))

  (attrs
    [element]
    (-> (to-edn element)
        :attrs))

  (text
    [element]
    (.text element))

  (data
    [element]
    (.data element)))

(defn one?
  [^Elements elements]
  (= 1 (.size elements)))

(extend-protocol Extractable
  Elements
  (jsoup
    [elements]
    (if (one? elements)
      (first elements)
      elements))

  (edn
    [elements]
    (if (one? elements)
      (edn (first elements))
      (map edn elements)))

  (tag
    [elements]
    (if (one? elements)
      (tag (first elements))
      (map tag elements)))

  (attr*
    [elements attribute]
    (if (one? elements)
      (attr* (first elements) attribute)
      (map #(attr* % attribute) elements)))

  (attrs
    [elements]
    (if (one? elements)
      (attrs (first elements))
      (map attrs elements)))

  (text
    [elements]
    (if (one? elements)
      (text (first elements))
      (map text elements)))

  (data
    [elements]
    (if (one? elements)
      (data (first elements))
      (map data elements))))

(extend-protocol Extractable
  TextNode
  (jsoup
    [node]
    node)

  (edn
    [node]
    (to-edn node))

  (tag
    [node]
    nil)

  (attr*
    [node attribute]
    nil)

  (attrs
    [node]
    nil)

  (text
    [node]
    (.getWholeText node))

  (data
    [node]
    nil))

(defn attr
  "Convenience function allowing:

     (chain (attr :href) ..)

   instead of requiring:

     (chain #(attr* % :href) ..)"
  ([attribute] (fn [x] (attr* x attribute)))
  ([x attribute] (attr* x attribute)))

(defn chain
  "Executes the supplied functions in left to right order
   on an element.

     (chain text parse-date)"
  [& fns]
  (fn [x]
    ((apply comp (reverse fns)) x)))

;;
;; Format Data Extractions
;;

(defn run-extraction
  [source sel extraction]
  (let [[selector extract] extraction
        selected (sel source selector)]
    (extract selected)))

(defn run-extractions
  [source extractions]
  (let [sel (memoize select)]
    (case (count extractions)
      0 (to-edn source)
      1 (run-extraction source sel (first extractions))
      (map (partial run-extraction source sel) extractions))))

(defn extract
  "ks is a vector of keys that will be zipped into a map
   with the extracted data, ie:

     {(first ks) (first (run-extractions source extractions))}

   If ks is nil or empty, a sequence of extracted data will
   be returned instead.

   Extractions are a selector (see `select`) followed by an
   extractor (see `Extractable`).

     (extract (parse subreddit)
              [:headlines]
              \".sitetable .thing .title a.title\" text)"
  [source ks & extractions]
  (let [extractions (partition 2 extractions)
        extracted (run-extractions source extractions)]
    (if (empty? ks)
      extracted
      (case (count extractions)
        0 {(first ks) extracted}
        1 {(first ks) extracted}
        (zipmap ks extracted)))))

(defn extract-from
  "Behaves like extract, but prior to running extractions
   uses the provided selector to narrow down the data to
   be searched.

   This is useful, for instance, when one wants to select
   a sequence of items, then extract identical information
   from each."
  [source selector ks & extractions]
  (let [sources (select source selector)]
    (map #(apply (partial extract % ks) extractions) sources)))
