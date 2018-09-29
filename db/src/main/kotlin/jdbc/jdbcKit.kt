package jdbc

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import klib.base.MDCCloseable
import klib.base.WithLogging
import klib.base.md5Sum
import klib.base.trimToDefault
import org.flywaydb.core.Flyway
import javax.sql.DataSource

interface ISimpleDataSourceFactory {
    val jdbcUrlString: String
    val login: String
    val newDataSource: DataSource
    fun with(login: String, pw: String): DataSource
}

abstract class SimpleDataSourceFactory(val jdbcDriver: String, jdbcPrefix: String, hostAndPort: String, dbName: String,
                                       override val login: String, protected val pw: String) : ISimpleDataSourceFactory {
    override val jdbcUrlString = "$jdbcPrefix://$hostAndPort/$dbName"
}

class HikariDataSourceFactory(jdbcDriver: String, jdbcPrefix: String, hostAndPort: String, dbName: String,
                              login: String, pw: String) :
        SimpleDataSourceFactory(jdbcDriver, jdbcPrefix, hostAndPort, dbName, login, pw) {

    companion object : WithLogging()

    private fun newConfig(login: String, pw: String) = HikariConfig().apply {
        driverClassName = jdbcDriver
        jdbcUrl = jdbcUrlString
        maximumPoolSize = 5
        connectionTestQuery = "SELECT 1"
//        setPoolName("springHikariCP")

        addDataSourceProperty("dataSource.cachePrepStmts", "true")
        addDataSourceProperty("dataSource.prepStmtCacheSize", "250")
        addDataSourceProperty("dataSource.prepStmtCacheSqlLimit", "2048")
        addDataSourceProperty("dataSource.useServerPrepStmts", "true")

        username = login
        password = pw
        LOG.warn("[newDataSource] Creating datasource $username:${(login to pw).md5Sum}" +
                " @ $jdbcUrlString")

    }

    override fun with(login: String, pw: String) = newConfig(login, pw).let { HikariDataSource(it) }

    override val newDataSource: DataSource
        get() = with(login, pw)

}

/**
 * @managedSchemas Default is [parent.login, "public"]. If set to [""], then [parent.login] will be used
 */
class FlywayDataSourceFactory(val parent: ISimpleDataSourceFactory, val adminLogin: String, private var adminPw: String,
                              val sqlLocations: Array<out String> = arrayOf("flyway"),
                              vararg managedSchemas: String = arrayOf(parent.login, "public")
) : ISimpleDataSourceFactory by parent {

    companion object : WithLogging()

    val _managedSchemas = when {
        managedSchemas.contentEquals(arrayOf("")) -> arrayOf(parent.login)
        else -> managedSchemas
    }

    override val newDataSource: DataSource
        @Synchronized get() = handleMigrations().let {
            parent.newDataSource
        }

    private var migrationProcessed = false

    private fun handleMigrations() {

        if (migrationProcessed) return

        val adminDS = parent.with(adminLogin, adminPw)
        adminPw = ""
        try {
            Flyway().apply {

                dataSource = adminDS
                setSchemas(*_managedSchemas)
//                setDataSource(jdbcUrlString, adminLogin, adminPw, "")
                setLocations(*sqlLocations)
                sqlMigrationSeparator = "#"

                installedBy = System.getProperty("user.name") + " (" + System.getProperty("os.name") + " " +
                        System.getProperty("os.version") + ")"

                MDCCloseable().put("system-user", installedBy).use {
                    val deleteAll = System.getenv("FLYWAY_DELETE_ALL")
                            .trimToDefault(System.getProperty("flyway.delete-all", "false")).toBoolean()
                    if (deleteAll) {
                        LOG.error("[handleMigrations] flyway.delete-all|FLYWAY_DELETE_ALL: TRUE; Cleaning the database")
                        clean()
                    }

                    LOG.warn("[handleMigrations] Migrating...")
                    migrate()
                }


            }

        } finally {
            migrationProcessed = true
            (adminDS as? HikariDataSource)?.close()
        }

    }

}
