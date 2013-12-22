package basic

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import org.slf4j.LoggerFactory

/**
 * Base class for performance tests.
 */
class SimulationBase extends Simulation {

  val logger = LoggerFactory.getLogger(getClass)

  //read settings
  val protocol: String = Option(System.getProperty("protocol")).getOrElse("http")
  val host: String = Option(System.getProperty("host")).getOrElse("localhost")
  val port: Int = Integer.getInteger("port",80)
  val users: Int = Integer.getInteger("users", 1)
  val rampup: Long = java.lang.Long.getLong("rampup", 0L)
  val testduration: Int = Integer.getInteger("duration", 1)
  val maxresptime: Int = Integer.getInteger("maxresptime", 2000)
  val successpercent: Int = Integer.getInteger("successpercent", 100)

  val baseUrl = protocol + "://" + host + ":" + port + "/"

  //set host and headers
  val httpConf = http
    .baseURL(baseUrl)
    .acceptCharsetHeader("ISO-8859-1,utf-8;q=0.7,*;q=0.7")
    .acceptHeader("text/html,application/xhtml+xml,application/xml,application/x-gzip;q=0.9,*/*;q=0.8")
    .acceptEncodingHeader("gzip, deflate")
    .acceptLanguageHeader("en,en-en;q=0.8,en-us;q=0.5,en;q=0.3")
    .disableFollowRedirect
    .disableCaching

}