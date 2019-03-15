package example

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.cert.X509Certificate
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.Logger
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

private val logger = Logger.getLogger("main")

fun main() {
    System.setProperty(
        "java.util.logging.SimpleFormatter.format",
        "[%1\$tF %1\$tT.%1\$tL] [%4\$s] %5\$s %n"
    )

    logger.info("Starting...")


    val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
        override fun getAcceptedIssuers(): Array<X509Certificate>? = null
        override fun checkClientTrusted(certs: Array<X509Certificate>, authType: String) {}
        override fun checkServerTrusted(certs: Array<X509Certificate>, authType: String) {}
    })

    val sslContext = SSLContext.getInstance("SSL")
    sslContext.init(null, trustAllCerts, java.security.SecureRandom())
    HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.socketFactory)

    val client = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_2)
        .connectTimeout(Duration.ofSeconds(20))
        .sslContext(sslContext)
        .build()


    val request = HttpRequest.newBuilder(URI("https://localhost:4430/push"))
        .GET()
        .build()

    val counter = AtomicInteger(1)

    client.sendAsync(
        request,
        HttpResponse.BodyHandlers.ofString(),
        HttpResponse.PushPromiseHandler { _, pushPromiseRequest, acceptor ->
            logger.info("pushPromiseRequest: ${pushPromiseRequest.uri()}")
            acceptor
                .apply(HttpResponse.BodyHandlers.ofString())
                .thenAcceptAsync { response ->
                    logger.info("${counter.getAndIncrement()}/18 ${response.uri()}")
                }
        }
    ).exceptionally {
        it.printStackTrace()
        null
    }

    try {
        System.`in`.read()
    } catch (e: Exception) {
    }
}
