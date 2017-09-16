package devnull

import java.io.File
import java.time.Clock

import com.typesafe.scalalogging.LazyLogging
import devnull.ems.{CachingEmsService, EmsHttpClient, EmsService}
import devnull.storage._
import doobie.contrib.hikari.hikaritransactor.HikariTransactor
import unfiltered.jetty.Server

import scala.util.Properties._
import scalaz.concurrent.Task

case class AppConfig(
    httpPort: Int,
    httpContextPath: String,
    home: File,
    databaseConfig: DatabaseConfig,
    emsUrl: String)

case class AppReference(server: Server)

object Jetty extends InitApp[AppConfig, AppReference] {


  override def onStartup(): AppConfig = {
    val config: AppConfig = createConfig()
    Migration.runMigration(config.databaseConfig)
    config
  }

  override def onStart(cfg: AppConfig): AppReference = {
    val dbCfg: DatabaseConfig = cfg.databaseConfig
    val xa = for {
      xa <- HikariTransactor[Task](dbCfg.driver, dbCfg.connectionUrl, dbCfg.username, dbCfg.password)
      _ <- xa.configure(hxa =>
        Task.delay {
          hxa.setMaximumPoolSize(10)
        })
    } yield xa

    val repository: FeedbackRepository = new FeedbackRepository()
    val paperFeedbackRepository: PaperFeedbackRepository = new PaperFeedbackRepository()
    implicit val clock = Clock.systemUTC()
    val emsService: EmsService = new CachingEmsService(new EmsHttpClient(cfg.emsUrl))

    val server = unfiltered.jetty.Server.http(cfg.httpPort).context(cfg.httpContextPath) {
      _.plan(Resources(emsService, repository, paperFeedbackRepository, xa.run))
    }.requestLogging("access.log")

    server.underlying.setSendDateHeader(true)
    server.run(_ => logger.info(s"Running server at ${cfg.httpPort}${cfg.httpContextPath}"))
    AppReference(server)
  }

  override def onShutdown(refs: AppReference): Unit = {}

  def createConfig(): AppConfig = {
    val port = envOrElse("PORT", "8082").toInt
    val contextPath = propOrElse("contextPath", envOrElse("CONTEXT_PATH", "/server"))
    val home = new File(propOrElse("app.home", envOrElse("app.home", ".")))
    val emsUrl = propOrElse("emsUrl", envOrElse("EMS_URL", "http://test.javazone.no/ems/server/"))
    logger.info(s"port ${port}")
    logger.info(s"contextPath ${contextPath}")
    logger.info(s"home ${home}")
    logger.info(s"emsUrl ${emsUrl}")

    val dbConfig = DatabaseConfigEnv()
    AppConfig(port, contextPath, home, dbConfig, emsUrl)
  }

}

trait InitApp[C, R] extends App with LazyLogging {

  def onStartup(): C

  def onStart(cfg: C): R

  def onShutdown(refs: R)

  logger.debug("onStartup")
  val cfg = onStartup()
  logger.debug("onStart")
  val refs = onStart(cfg)
  logger.debug("onShutdown")
  onShutdown(refs)
}