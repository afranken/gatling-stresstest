package basic

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import bootstrap._
import assertions._
import io.gatling.http.response
import io.gatling.http.response.DelegatingReponse
import java.io.ByteArrayOutputStream
import org.apache.commons.io.IOUtils
import org.apache.commons.codec.binary.StringUtils
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream

/**
 * Performance Test that requests URIs read from a sitemap
 */
class SitemapSimulation extends SimulationBase {

  //read settings
  val sitemap: String = Option(System.getProperty("sitemap")).getOrElse("sitemap")
  val xpathselector: String = Option(System.getProperty("xpathselector")).getOrElse("/sm:sitemapindex/sm:sitemap/sm:loc")

  //create scenario, requests are executed for the configured duration for all URIs found in the Sitemap
  val scn = scenario("Sitemap")
    //request sitemap, save all links found in URI_LIST
    .exec(http(sitemap)
    .get(sitemap)
    .transformResponse(transformGzippedResponse)
    .check(xpath(xpathselector, List("sm" -> "http://www.sitemaps.org/schemas/sitemap/0.9")).findAll.whatever.saveAs("URI_LIST"))
    )
    .exec((s: Session) => {
    logger.debug("URIs found: {}", s.attributes.get("URI_LIST"))
    s
  })
    .doIfOrElse(session => session.contains("URI_LIST")) {
    //crawl list for configured amount of time
    during(testduration minutes) {
      exec(foreach("${URI_LIST}", "URI") {
        exec(http("${URI}")
          .get("${URI}")
        )
      })
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


  /**
   * Unpack response body in case a Gzipped sitemap was requested.
   * @return a non-gzipped response
   */
  def transformGzippedResponse: response.ResponseTransformer = {
    case response if response.getStatusCode == 200 =>

      new DelegatingReponse(response) {

        def isGzipped: Boolean = "application/x-gzip".equals(response.getContentType)

        override def getResponseBodyAsStream() = if (isGzipped) {
          new GzipCompressorInputStream(response.getResponseBodyAsStream)
        } else {
          response.getResponseBodyAsStream
        }

        override def getResponseBodyAsBytes() = if (isGzipped) {
          val bytesOut = new ByteArrayOutputStream(512)
          IOUtils.copy(getResponseBodyAsStream(), bytesOut)
          bytesOut.toByteArray
        } else {
          response.getResponseBodyAsBytes
        }

        override def getContentType() = if (isGzipped) {
          "application/xml"
        } else {
          response.getContentType
        }

        override def getResponseBody() = if (isGzipped) {
          StringUtils.newStringUtf8(getResponseBodyAsBytes())
        } else {
          response.getResponseBody
        }

        override def getResponseBody(charset: String) = if (isGzipped) {
          StringUtils.newString(getResponseBodyAsBytes(), charset)
        } else {
          response.getResponseBody(charset)
        }
      }
  }
}