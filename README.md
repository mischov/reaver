# Reaver

Reaver is a Clojure library wrapping Jsoup and designed for extracting data out of HTML and into EDN/Clojure.

Here is how one might scrape the headlines and links from Hacker News into a Clojure map (though Hacker News has an API and you should use that):

```Clojure
(require '[reaver :refer [parse extract-from text attr]])

; Reaver doesn't tell you how fetch your HTML. Use `slurp` or
; aleph or clj-http or what-have-you.
(def hacker-news (slurp "https://news.ycombinator.com/"))

; Extract the headlines and urls from the HTML into a seq of maps.
(extract-from (parse hacker-news) ".itemlist .athing"
              [:headline :url]
              ".title > a" text
              ".title > a" (attr :href))

;> ({:headline "...", :url "..."}, {:headline "...", :url "..."}, ...)
```

## Contents

- [Installation](#installation)
- [Why?](#why)
- [Status](#status)


## Installation

Add the following dependency to your project.clj file:

```clojure
[reaver "0.1.3"]
```
[**Back To Top ⇧**](#contents)


## Why?

Clojure doesn't have a simple, purposed library for extracting data from HTML and into EDN.

The libraries most commonly used for this, Enlive and Laser, are primarily templating libraries, and extracting data from the data.xml format (like that created by Crouton) can be complicated and/or slow.

Reaver leverages Jsoup's well-fleshed-out selection and extraction mechanisms to provide an easy API for extracting data from XML.

[**Back To Top ⇧**](#contents)

## Status

Reaver is in **early beta**.

Since Reaver is primarily a wrapper around the battle-tested Jsoup library and otherwise a small, simple library, you should be able to use Reaver in production (with caution, of course).

There may still be lurking bugs or inconsistencies, so please report if you encounter one.

[**Back To Top ⇧**](#contents)

## License

Copyright © 2014 Mischov

Distributed under the Eclipse Public License.
