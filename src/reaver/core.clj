(ns reaver.core
  (:require [clojure.string :as string])
  (:import [org.jsoup Jsoup]
           [org.jsoup.parser Parser Tag]
           [org.jsoup.nodes Attribute Attributes Comment DataNode
                            Document DocumentType Element Node
                            TextNode]
           [org.jsoup.select Elements]))

;;-------------------------------------------------------------------
;; Parse HTML

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

;;-------------------------------------------------------------------
;;  Jsoup->EDN

(defn to-keyword
  "Converts a string into a lowercase keyword."
  [s]
  (-> s string/lower-case keyword))

(defprotocol EDNable
  (to-edn [jsoup]
    "Converts Jsoup into an edn representation of HTML.

       {:type    Keyword
        :tag     Keyword|nil
        :attrs   {Keyword String, ...}|nil
        :content Vector|Map|String|nil}"))

(extend-protocol EDNable
  Attribute
  (to-edn [attr]
    [(to-keyword (.getKey attr)) (.getValue attr)])
  
  Attributes
  (to-edn [attrs]
    (not-empty (into {} (map to-edn attrs))))

  Comment
  (to-edn [comment]
    {:type :comment
     :content [(.getData comment)]})

  DataNode
  (to-edn [node]
    (str node))

  Document
  (to-edn [doc]
    {:type :document
     :content (not-empty (into [] (map to-edn (.childNodes doc))))})

  DocumentType
  (to-edn [doctype]
    {:type :document-type
     :attrs (to-edn (.attributes doctype))})

  Element
  (to-edn [element]
    {:type :element
     :attrs (to-edn (.attributes element))
     :tag (to-keyword (.tagName element))
     :content (not-empty (into [] (map to-edn (.childNodes element))))})

  Elements
  (to-edn [elements]
    (when (> (.size elements) 0)
      (map to-edn elements)))

  TextNode
  (to-edn [node]
    (.getWholeText node)))

;;  Select Elements

(defprotocol Selectable
  (select* [node css-selector]))

(extend-protocol Selectable
  Document
  (select* [document css-selector]
    (.select document ^String css-selector))
  
  Elements
  (select* [elements css-selector]
    (.select elements ^String css-selector))

  Element
  (select* [element css-selector]
    (.select element ^String css-selector)))

(defn select
  "Given HTML parsed to Jsoup and a string representing
   a CSS-esque selector, select* returns Jsoup representing
   any successfully selected data.

   For more on selector syntax, see:
   http://jsoup.org/cookbook/extracting-data/selector-syntax"
  [node css-selector]
  (let [^Elements result (select* node css-selector)]
    (if (.isEmpty result)
      nil
      result)))

;;-------------------------------------------------------------------
;;  Extract Data

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
  (jsoup [_] nil) 
  (edn [_] nil)
  (tag [_] nil)
  (attr* [_ _] nil)
  (attrs [_] nil)
  (text [_] nil)
  (data [_] nil))

(extend-protocol Extractable
  Element
  (jsoup [element] element)
  (edn [element] (to-edn element))
  (tag [element] (-> element to-edn :tag))
  (attr* [element attribute]
    (-> element to-edn (get-in [:attrs (keyword attribute)])))
  (attrs [element] (-> element to-edn :attrs))
  (text [element] (.text element))
  (data [element] (.data element)))

(defn one-element?
  [^Elements elements]
  (= 1 (.size elements)))

(extend-protocol Extractable
  Elements
  (jsoup [elements]
    (if (one-element? elements)
      (first elements)
      elements))
  (edn [elements]
    (if (one-element? elements)
      (edn (first elements))
      (map edn elements)))
  (tag [elements]
    (if (one-element? elements)
      (tag (first elements))
      (map tag elements)))
  (attr* [elements attribute]
    (if (one-element? elements)
      (attr* (first elements) attribute)
      (map #(attr* % attribute) elements)))
  (attrs [elements]
    (if (one-element? elements)
      (attrs (first elements))
      (map attrs elements)))
  (text [elements]
    (if (one-element? elements)
      (text (first elements))
      (map text elements)))
  (data [elements]
    (if (one-element? elements)
      (data (first elements))
      (map data elements))))

(defn attr
  "Convinience function allowing:

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

;;-------------------------------------------------------------------
;;  Format Data Extractions

(defn run-extraction
  [source sel [selector extractor]]
  (let [selection (sel source selector)]
    (extractor selection)))

(defn run-labeled-extraction
  [source sel [label selector extractor]]
  [label (run-extraction source sel [selector extractor])])

(defn extract
  "Extract expects extractions to be a selector (see `select`)
   followed by an extractor (see `Extractable`).

   Source must be parsed with parse or parse-fragment.

   Example:

     (extract (parse subreddit)
              \".sitetable .thing .title a.title\" text)

   Returns: A seq of results."
  [source & extractions]
  (assert (seq extractions) "Must provide extractions.")
  (let [extractions' (partition 2 extractions)
        sel (memoize select)]
    (map #(run-extraction source sel %) extractions')))

(defn extract-as
  "Extract-as expects extractions to be a label (to be used as
   a map key), a selector (see `select`), and an extractor
   (see `Extractable`).

   Source must be parsed with parse or parse-fragment.

   Example:

     (extract (parse subreddit)
              :headline \".sitetable .thing .title a.title\" text)

   Returns: A map of labels to results."
  [source & extractions]
  (assert (seq extractions) "Must provide extractions.")
  (let [extractions' (partition 3 extractions)
        sel (memoize select)]
    (into {} (map #(run-labeled-extraction source sel %) extractions'))))

(defn extract-from
  "Behaves like extract, but uses the provided selector on
   the source and then maps the extractions over the results.

   Example:

     (extract-from (parse subreddit) \".sitetable .thing\"
                   \".title a.title\" text)

   Returns: A seq of result seqs."
  [source selector & extractions]
  (let [sources (select source selector)]
    (map #(apply extract % extractions) sources)))

(defn extract-from-as
  "Behaves like extract-as, but uses the provided selector on
   the source and then maps the extractions over the results.

   Example:

     (extract-from (parse subreddit) \".sitetable .thing\"
                   :headline \".title a.title\" text)

   Returns: A seq of result maps."
  [source selector & extractions]
  (let [sources (select source selector)]
    (map #(apply extract-as % extractions) sources)))
