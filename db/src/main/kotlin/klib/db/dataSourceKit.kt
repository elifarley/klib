package klib.db

import jdbc.FlywayDataSourceFactory
import jdbc.HikariDataSourceFactory
import jdbc.ISimpleDataSourceFactory
import klib.base.trimToDefault
import org.koin.dsl.module.Module
import org.koin.dsl.module.applicationContext

val dsModule: Module = applicationContext {

    bean {
        HikariDataSourceFactory(
                getProperty("db.driver"),
                getProperty("db.prefix"),
                System.getenv("DB_HOST").trimToDefault(getProperty("db.host-and-port")),
                System.getenv("DB_NAME").trimToDefault(getProperty("db.name")),
                System.getenv("DB_LOGIN").trimToDefault(getProperty("db.login")),
                System.getenv("DB_PW")
        ) as ISimpleDataSourceFactory
    }

    bean {
        FlywayDataSourceFactory(get<ISimpleDataSourceFactory>(),
                System.getenv("DB_ADMIN_LOGIN").trimToDefault(getProperty("db.admin.login")),
                System.getenv("DB_ADMIN_PW")
        ).newDataSource
    }

}

