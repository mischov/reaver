# Reaver

Reaver is a Clojure library wrapping Jsoup and designed for extracting data out of HTML and into EDN/Clojure.

Here is how one might scrape the headlines and links from r/Clojure into a Clojure map:

```Clojure
(require '[reaver.core :as reaver])

; Reaver doesn't tell you how fetch your HTML. Use `slurp` or
; aleph or clj-http or whatever you would like.
(def rclojure (slurp "https://www.reddit.com/r/clojure"))

; Extract the headlines and urls from the HTML into a seq of maps.
(reaver/extract-from (reaver/parse rclojure) ".sitetable .thing"
                     :headline ".title a.title" reaver/text
                     :url      ".title a.title" (reaver/attr :href))

;> ({:headline "...", :url "..."}, {:headline "...", :url "..."}, ...)
```

## Contents

- [Installation](#installation)
- [Rationale](#rationale)
- [Status](#status)
- [API](#api)


## Installation

Add the following dependency to your project.clj file:

```clojure
[reaver "0.1.2"]
```
[**Back To Top ⇧**](#contents)


## Rationale

Reaver is a simple, purposed library for extracting data from HTML and into EDN.

The libraries most commonly used for this, Enlive and Laser, are primarily templating libraries, and extracting data from the data.xml format (like that created by Crouton) can be complicated and/or slow.

Reaver wraps Jsoup's well-fleshed-out selection and extraction mechanisms in a simple Clojure API for extracting data from XML. 

[**Back To Top ⇧**](#contents)

## Status

Reaver is in **early beta**.

Since Reaver is primarily a wrapper around the battle-tested Jsoup library and otherwise a small, simple library, you should be able to use Reaver in production (with caution, of course).

There may still be lurking bugs or inconsistencies, so please report if you encounter one.

[**Back To Top ⇧**](#contents)

## API

- [Selectors](#selectors)
- [Extractors](#extractors)
- [**`parse`**](#parse)
- [**`parse-fragment`**](#parse-fragment)
- [**`extract`**](#extract)
- [**`extract-from`**](#extract-from)

#### Selectors

Reaver leverages Jsoup's selection format, which is css-esque selector strings.

For more information, see Jsoup's [selector syntax documentation](http://jsoup.org/cookbook/extracting-data/selector-syntax).

#### Extractors

Reaver provides several basic extraction functions to help you coerce selections into a useful format: `jsoup edn tag attrs attr* attr text data chain`

```Clojure
(def parsed-html
 (reaver/parse-fragment "<a class=\"clickable\" href=\"https://www.google.com/\">Google</a>"))

(reaver/jsoup parsed-html)
;> #object[org.jsoup.nodes.Element ... "<a class=\"clickable\" href=\"https://www.google.com/\">Google</a>"]

(reaver/edn parsed-html)
;> {:type :element,
;   :attrs {:class "clickable", :href "https://www.google.com/"},
;   :tag :a,
;   :content ["Google"]}

(reaver/tag parsed-html) 
;> :a

(reaver/attrs parsed-html) 
;> {:class "clickable", :href "https://www.google.com"}

(reaver/attr parsed-html :class)
;> "clickable"

((reaver/attr :class) parsed-html) 
;> "clickable"

(reaver/text parsed-html) 
;> "Google"

;; data

;; chain


```

####`parse`

Parses a string of HTML representing a full HTML document into Jsoup.

####`parse-fragment`

Parses a string representing a fragment of HTML into Jsoup.

####`extract`

Extract takes a parsed source and a variable number of extractions, and returns a map of results.

The first arg of an extraction is a key that the results of extraction will be stored under in the result map, the second arg is a selector, and the third arg is an extractor fn.

```Clojure
(reaver/extract (reaver/parse clojure-reddit)
                :headlines ".sitetable .thing .title a.title" reaver/text)

;> {:headlines ("..." "..." "..." ...)}
```

####`extract-from`

Extract-from takes a parsed source, a selector, and a variable number extractions, and returns a seq of result maps.

The extractions are run with extract on the result of calling the selector on the source.

One example use of this behavior is to select a list of items and then extract identical information from each.

```Clojure
(reaver/extract-from (reaver/parse clojure-reddit) ".sitetable .thing"
                     :headline ".title a.title" reaver/text)

;> ({:headline "..."} {:headline "..."} {:headline "..."} ...)
```


[**Back To Top ⇧**](#contents)

## License

Copyright © 2015 Mischov

Distributed under the Eclipse Public License.
