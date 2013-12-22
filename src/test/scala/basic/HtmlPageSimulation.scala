package basic

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import bootstrap._
import assertions._

/**
 * Performance Test that requests URIs read from a sitemap
 */
class HtmlpageSimulation extends SimulationBase {

  //read settings
  val htmlpage: String = Option(System.getProperty("htmlpage")).getOrElse("schlagzeilen/")
  val cssselector: String = Option(System.getProperty("cssselector")).getOrElse("div[class~=schlagzeilen-content] > a")
  val linkattribute: String = Option(System.getProperty("linkattribute")).getOrElse("href")

  //create scenario, requests are executed for the configured duration for all URIs found in the Sitemap
  val scn = scenario("HtmlPage")
    //request sitemap, save all links found in URI_LIST
    .exec(http(htmlpage)
    .get(htmlpage)
    .check(css(cssselector, linkattribute).findAll.exists.saveAs("URI_LIST"))
    )
    .exec((s: Session) => {
    logger.debug("URIs found: {}", s.attributes.get("URI_LIST"))
    s
  })
    .doIfOrElse(session => session.contains("URI_LIST")) {
    //crawl list for configured amount of time
    during(testduration minutes) {
      exec(
        foreach("${URI_LIST}", "URI") {
          exec(http("${URI}")
            .get("${URI}")
          )
        }
      )
    }
  } {
    //no URIs found.
    exec((s: Session) => {
      logger.error("no URIs found.")
      s
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