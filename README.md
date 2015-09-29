# Reaver

Reaver is a Clojure library wrapping Jsoup and designed for extracting data out of HTML and into EDN/Clojure.

Here is how one might scrape the headlines and links from r/Clojure into a Clojure map:

```Clojure
(require '[reaver.core :as reaver])

; Reaver doesn't tell you how fetch your HTML. Use `slurp` or
; aleph or clj-http or what-have-you.
(def rclojure (slurp "https://www.reddit.com/r/clojure"))

; Extract the headlines and urls from the HTML into a seq of maps.
(reaver/extract-from-as (reaver/parse rclojure) ".sitetable .thing"
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

Clojure doesn't have a simple, purposed library for extracting data from HTML and into EDN.

The libraries most commonly used for this, Enlive and Laser, are primarily templating libraries, and extracting data from the data.xml format (like that created by Crouton) can be complicated and/or slow.

Reaver leverages Jsoup's well-fleshed-out selection and extraction mechanisms to provide an easy API for extracting data from XML. 

[**Back To Top ⇧**](#contents)

## Status

Reaver is in **early beta**.

Since Reaver is primarily a wrapper around the battle-tested Jsoup library and otherwise a small, simple library, you should be able to use Reaver in production (with caution, of course).

There may still be lurking bugs or inconsistencies, so please report if you encounter one.

[**Back To Top ⇧**](#contents)

## API

- [`parse`](#parse)
- [`parse-fragment`](#parse-fragment)
- [`extract`](#extract)
- [`extract-as`](#extract-as)
- [`extract-from`](#extract-from)
- [`extract-from-as`](#extract-from-as)

###`parse`

Parses a string of HTML representing a full HTML document into Jsoup.

###`parse-fragment`

Parses a string representing a fragment of HTML into Jsoup.

###`extract`

```Clojure
(reaver/extract (reaver/parse clojure-reddit)
                ".sitetable .thing .title a.title" reaver/text)

;> ("..." "..." "..." ...)
```

###`extract-as`

```Clojure
(reaver/extract-as (reaver/parse clojure-reddit)
                   :headlines ".sitetable .thing .title a.title" reaver/text)

;> {:headlines ("..." "..." "..." ...)}
```

###`extract-from`

```Clojure
(reaver/extract-from (reaver/parse clojure-reddit) ".sitetable .thing"
                     ".title a.title" reaver/text)

;> ("..." "..." "..." ...)
```

###`extract-from-as`

```Clojure
(reaver/extract-from-as (reaver/parse clojure-reddit) ".sitetable .thing"
                        :headline ".title a.title" reaver/text)

;> ({:headline "..."} {:headline "..."} {:headline "..."} ...)
```

[**Back To Top ⇧**](#contents)

## License

Copyright © 2015 Mischov

Distributed under the Eclipse Public License.
