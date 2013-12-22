package basic

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import bootstrap._
import assertions._

/**
 * Performance Test that requests URIs read from a CSV file
 */
class CsvSimulation extends SimulationBase {

  //read specific settings
  val urilistcsv: String = Option(System.getProperty("urilistcsv")).getOrElse("urilist.csv")

  //iterate over the URIs found in the CSV
  val accessLog = csv(urilistcsv).circular

  //create scenario, requests are executed for the configured duration for all URIs found in the CSV file
  val scn = scenario("CSV Test")
  .during(testduration minutes) {
    feed(accessLog)
    .exec(http("${uri}")
      .get("${uri}"))
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