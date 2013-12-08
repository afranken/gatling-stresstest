package basic

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import bootstrap._
import assertions._

/**
 * Performance Test that requests URIs read from a sitemap
 */
class SitemapSimulation extends SimulationBase {

  //read settings
  val sitemap: String = Option(System.getProperty("sitemap")).getOrElse("sitemap")
  val cssselector: String = Option(System.getProperty("cssselector")).getOrElse("div[class~=sitemap-content] > h4 > a")

  //create scenario, requests are executed for the configured duration for all URIs found in the Sitemap
  val scn = scenario("Sitemap")
    //request sitemap, save all links found in URI_LIST
    .exec(http(sitemap)
    .get(sitemap)
    .check(css(cssselector, "href").findAll.whatever.saveAs("URI_LIST"))
    )
    //crawl list for configured amount of time
    .during(testduration minutes) {
    exec(foreach("${URI_LIST}", "URI") {
      exec(http("""${URI}""")
        .get("""${URI}""")
        .check(status.is(200))
      )
    })
  }

  //actually start test
  setUp(
    scn.inject(
      ramp(users users)
        over (rampup seconds)
    )
  )
  .protocols(httpConf)
  .assertions(
    global.successfulRequests.percent.is(successpercent),
    details(baseUrl).responseTime.max.lessThan(maxresptime)
  )
}