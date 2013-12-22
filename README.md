gatling-stresstest
==================

A configurable website stress test based on gatling.
All tests run against "[spiegel.de](http://www.spiegel.de)" by default.

##Run the test

Start with `mvn clean verify -Pperformance`
Overwrite parameters either permanently in the `<properties>` element of the [pom.xml](<pom.xml>) or temporarily by passing system properties, e.g. `mvn clean verify -Pperformance -Dtestname=HtmlpageSimulation -Dduration=5`

**Configurable Parameters:**

* `protocol` => the protocol to use. Default: "http"
* `host` => the host to use. Default: "www.spiegel.de"
* `port` => the port to use. Default: "80"
* `users` => the number of users / threads to use in the test. Default: "5"
* `rampup` => rampup/rampdown in seconds, will be added to test duration. Default: "10"
* `duration` => duration of the test in minutes. Default: "1"
* `maxresptime` => maximum time in seconds a response may take to count as a success. Default: "2000"
* `successpercent` => percentage of responses that must be successful, otherwise fail the build. Default: "100"
* `testname` => Name of the test class. Default: "CsvSimulation"

##CsvSimulation
The default test.
Loads `src/test/resources/data/urilist.csv` and loops over the URIs.

**Configurable Parameters:**

* `urilistcsv` => name of the CSV file to read the URIs from. Path must be relative to the configured "dataFolder". Default: "urilist.csv"

##HtmlpageSimulation
Loads a HTML page, selects HTML Elements with a CssSelector, and loops over the the URIs found in the linkattribute.

**Configurable Parameters:**

* `htmlpage` => relative URI to select links from. Default: "schlagzeilen/"
* `cssselector` => [Csselly](http://jodd.org/doc/csselly/) style CSS Selector to select the HTML Elements with. Default: "div[class~=schlagzeilen-content] > a"
* `linkattribute` => The attribute the links are taken from. Default: "href"

##SitemapSimulation
Loads a Sitemap XML, selects links with a XPath, and loops over the URIs.

**Configurable Parameters:**

* `sitemap` => relative URI of the Sitemap XML. Default: "sitemap.xml"
* `xpathselector` => XPath selector to select the links with. Attributes must be prepended with Namespace "sm". Default: "/sm:sitemapindex/sm:sitemap/sm:loc"