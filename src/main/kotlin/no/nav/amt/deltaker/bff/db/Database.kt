package no.nav.amt.deltaker.bff.db

import com.zaxxer.hikari.HikariDataSource
import kotliquery.Session
import kotliquery.sessionOf
import kotliquery.using
import no.nav.amt.deltaker.bff.Environment
import org.flywaydb.core.Flyway
import javax.sql.DataSource

object Database {
    lateinit var dataSource: DataSource

    fun init(environment: Environment) {
        dataSource = HikariDataSource().apply {
            dataSourceClassName = "org.postgresql.ds.PGSimpleDataSource"
            addDataSourceProperty("serverName", environment.dbHost)
            addDataSourceProperty("portNumber", environment.dbPort)
            addDataSourceProperty("databaseName", environment.dbDatabase)
            addDataSourceProperty("user", environment.dbUsername)
            addDataSourceProperty("password", environment.dbPassword)
            maximumPoolSize = 10
            minimumIdle = 1
            idleTimeout = 10001
            connectionTimeout = 1000
            maxLifetime = 30001
        }

        runMigration()
    }

    fun <A> query(block: (Session) -> A): A {
        return using(sessionOf(dataSource)) { session ->
            block(session)
        }
    }

    fun close() {
        (dataSource as HikariDataSource).close()
    }

    private fun runMigration(initSql: String? = null): Int =
        Flyway.configure()
            .connectRetries(5)
            .dataSource(dataSource)
            .initSql(initSql)
            .load()
            .migrate()
            .migrations
            .size
}
