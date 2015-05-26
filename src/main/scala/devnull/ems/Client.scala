package devnull.ems

import com.ning.http.client
import dispatch._
import net.hamnaberg.json.collection.JsonCollection

trait Client {
  def client(url: String) : Future[JsonCollection]
}

trait HttpClient extends Client {
  import scala.concurrent.ExecutionContext.Implicits.global
  val c: Http = new Http().configure(_.setFollowRedirects(true))

  def password: Option[String] = None

  def username: Option[String] = None

  def client(url: String) = {
    c(authUrl(url) OK Collection)
  }

  private def authUrl(path: String): Req = {
    if (username.isDefined && password.isDefined) {
      url(path).as_!(username.get, password.get)
    } else {
      url(path)
    }
  }
}

object Collection extends (client.Response => JsonCollection) {
  override def apply(r: client.Response): JsonCollection = {
    val parsed: Either[Throwable, JsonCollection] = JsonCollection.parse(r.getResponseBodyAsStream)
    parsed.right.get
  }
}
