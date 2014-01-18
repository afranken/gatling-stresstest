package stresstest

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
import java.nio.ByteBuffer

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
    .exec(
      http(sitemap).get(sitemap)
      .transformResponse(transformGzippedResponse)
      .check(
        xpath(xpathselector, List("sm" -> "http://www.sitemaps.org/schemas/sitemap/0.9"))
        .findAll
        //strip out hostname and protocol since the value saved to URI_LIST is later used as the name of the request
        //which in turn is used for a file name. The name may be longer than 255 characters if the URI is long.
        .transform(_.map(_.map(relativeUri)))
        .whatever
        .saveAs("URI_LIST")
      )
    )
    .exec((s: Session) => {
    logger.debug("URIs found: {}", s.attributes.get("URI_LIST"))
    s
  })
    .doIfOrElse(session => session.contains("URI_LIST")) {
    //crawl list for configured amount of time
    during(testduration minutes) {
      exec(foreach("${URI_LIST}", "URI") {
        val uri = "${URI}"
        exec(http(uri)
          .get(uri)
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
   * Get the relative URI from the given URL.
   * @param source the URL
   * @return a relative URI
   */
  def relativeUri(source: String): String = {
    if(source.contains("://")) {
      source.substring(nthOccurrence(source,'/',3))
    }
    else {
      //source was not an absolute URL, return source.
      source
    }
  }

  /**
   * Find nth occurrence of given char.
   * @param source String to check
   * @param c char to find
   * @param count occurrence of the char to find
   * @return index of the char
   */
  def nthOccurrence(source: String, c: Char, count: Int): Int = {
    var counter: Int = 0
    var i: Int = 0
    while (counter < count) {
      if (source.charAt(i) == c) {
        counter += 1
      }
      i += 1
    }

    i
  }


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

        override def getResponseBodyAsByteBuffer() = if(isGzipped) {
          ByteBuffer.wrap(getResponseBodyAsBytes())
        } else {
          response.getResponseBodyAsByteBuffer
        }
      }
  }
}