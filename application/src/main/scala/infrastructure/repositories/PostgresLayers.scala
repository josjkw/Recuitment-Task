package infrastructure.repositories

import javax.sql.DataSource

import scala.concurrent.ExecutionContext

import doobie.Transactor
import infrastructure.configuration.AppConfiguration
import io.github.gaelrenoux.tranzactio.doobie._
import org.postgresql.ds.PGSimpleDataSource
import zio.interop.catz._
import zio.{Task, ZIO, ZLayer}

object PostgresLayers {

  val liveConnection: ZLayer[DataSource, Nothing, Connection] = ZLayer.fromZIO {
    for {
      dataSource <- ZIO.service[DataSource]
      xa         <- ZIO.succeed(Transactor.fromDataSource[Task](dataSource, ExecutionContext.global))
    } yield xa
  }

  val liveDataSource: ZLayer[AppConfiguration, Nothing, PGSimpleDataSource] =
    ZLayer.fromZIO {
      for {
        config <- ZIO.serviceWith[AppConfiguration](_.postgres)
        datasource <- ZIO.succeed {
          val dataSource = new org.postgresql.ds.PGSimpleDataSource()
          dataSource.setURL(config.address)
          dataSource.setUser(config.user)
          dataSource.setPassword(config.password)
          dataSource
        }
      } yield datasource
    }

  val liveDatabase: ZLayer[DataSource, Nothing, Database] = Database.fromDatasource

}
