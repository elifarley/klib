package klib.db

import klib.json.SimpleObject
import kotliquery.*
import javax.sql.DataSource

fun DataSource.newSession(returnGeneratedKeys: Boolean = false) = sessionOf(this, returnGeneratedKeys)

/*
fun <R> DataSource.useNewSession(returnGeneratedKeys: Boolean = false, f: (Session) -> R)
        = using(newSession(returnGeneratedKeys), f)
*/

/**
 * returns a KotliQuery instance
 */
fun SQLQuery.asKQuery(table: String? = null) = queryOf(sqlTemplate?.format(table) ?: sql, *params)

abstract class SimpleSessionFactory<T, PK>(val ds: DataSource) {

    protected abstract fun _newRepoSession(s: Session): SimpleSession<T, PK>

    fun <R> newRepoSession(returnGeneratedKeys: Boolean = false, block: (SimpleRepository<T, PK>) -> R) =
        using(_newRepoSession(ds.newSession(returnGeneratedKeys))) {
            block(it)
        }

}

abstract class SimpleSession<T, PK>(protected val s: Session, protected val table: String) : SimpleRepository<T, PK>,
    AutoCloseable {

    abstract val rowExtractor: (Row) -> T

    open val selectById: String = "select * from $table where %s"
    private val existsById: String = "select exists (select false from $table where %s)"

    override fun close() = s.close()

    open val byIdQuery = { template: String, pk: PK ->
        SQLQuery(false, template.format("id = ?"), pk)
    }

    override fun exists(id: PK): Boolean = byIdQuery(existsById, id)
        .asKQuery(table).map { it.boolean(1) }.asSingle.let {
        s.run(it)!!
    }

    override fun find(id: PK): T? = byIdQuery(selectById, id)
        .asKQuery(table).map(rowExtractor).asSingle.let {
        s.run(it)
    }

    override fun findAll(query: SimpleQuery?): Sequence<T> = (
            query as SQLQuery? ?: SQLQuery(false, "select * from $table")
            ).let { q ->
        (q.asKQuery(table)).map(rowExtractor).asList.let {
            s.run(it).asSequence() // TODO improve kotliquery.Row to return sequence
        }
    }

    override fun delete(id: PK) = "delete from $table where %s".let {
        byIdQuery(it, id).asKQuery(table).asUpdate
    }.let {
        s.run(it) == 1
    }

    override fun deleteAll(query: SimpleQuery): Long = (query as SQLQuery).let { q ->
        q.asKQuery(table).asUpdate.let {
            s.run(it).toLong()
        }
    }

    override fun update(query: SimpleQuery): Long = (query as SQLQuery).let { q ->
        q.asKQuery(table).asUpdate.let {
            s.run(it).toLong()
        }
    }

    override fun toString(): String {
        return "repo($table)"
    }

}

open class SimpleObjectRepository<PK>(protected val sf: SimpleObjectSessionFactory<PK>) :
    SimpleRepository<SimpleObject<PK?>, PK> {

    override fun exists(id: PK): Boolean = sf.newRepoSession { r ->
        r.exists(id)
    }

    override fun find(id: PK): SimpleObject<PK?>? = sf.newRepoSession { r ->
        @Suppress("UNCHECKED_CAST")
        r.find(id) as SimpleObject<PK?>?
    }

    override fun findAll(query: SimpleQuery?): Sequence<SimpleObject<PK?>> = sf.newRepoSession { r ->
        @Suppress("UNCHECKED_CAST")
        r.findAll(query) as Sequence<SimpleObject<PK?>>
    }

    override fun insert(obj: SimpleObject<PK?>) = sf.newRepoSession { r ->
        @Suppress("UNCHECKED_CAST")
        r.insert(obj as SimpleObject<PK>)
    }

    override fun update(obj: SimpleObject<PK?>): Long = sf.newRepoSession { r ->
        r.update(obj as SimpleObject<PK>)
    }

    override fun update(query: SimpleQuery): Long = sf.newRepoSession { r ->
        r.update(query)
    }

    override fun delete(id: PK) = sf.newRepoSession { r ->
        r.delete(id)
    }

    override fun deleteAll(query: SimpleQuery) = sf.newRepoSession { r ->
        r.deleteAll(query)
    }

}