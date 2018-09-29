package klib.db

import com.jsoniter.JsonIterator
import com.jsoniter.output.JsonStream
import klib.base.WithLogging
import klib.json.SimpleObject
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import org.postgresql.util.PSQLException
import javax.sql.DataSource

fun SQLQuery.Companion.bySimpleObjectAttr(attr: String, value: Any?) = SQLQuery.byField("attrs->$attr", value)

fun SQLQuery.Companion.asSimpleObjectUpdateQuery(where: String, attrs: String, vararg vparams: Any?) = SQLQuery(false,
        "update %s set attrs = attrs || cast(? as JSONB) where $where", attrs, *vparams)

fun SQLQuery.asSimpleObjectUpdateQueryAction(table: String? = null) = queryOf(sql.format(table), *params).asUpdate

class SimpleObjectSessionFactory<PK>(ds: DataSource, val table: String) : SimpleSessionFactory<SimpleObject<PK>, PK>(ds) {
    override fun _newRepoSession(s: Session): SimpleSession<SimpleObject<PK>, PK> = SimpleObjectSession(s, table)
}

class SimpleObjectSession<PK>(s: Session, table: String) : SimpleSession<SimpleObject<PK>, PK>(s, table) {

    companion object : WithLogging()

    override val rowExtractor: (Row) -> SimpleObject<PK> = { row ->
        SimpleObject(row.any("id") as PK,
                JsonIterator.deserialize(row.string("attrs")).asMap(),
                row.zonedDateTime("created"),
                try { row.zonedDateTime("updated") } catch (e: PSQLException) { null }
        )
    }

    override fun insert(obj: SimpleObject<PK>): PK = try {

        JsonStream.serialize(obj.attrs).let { attrs ->
            if (obj.id == null) {
                val sql = "select public.insert_json('%s', cast(? as JSONB)) id".format(table)
                try {
                    @Suppress("UNCHECKED_CAST")
                    s.run(queryOf(sql, attrs).map { row -> row.long(1) }.asSingle)!! as PK

                } catch (e: Exception) {
                    val msg = "[insert] Unable to obtain generated ID ($e) [SQL: $sql]"
                    LOG.error(msg, e)
                    throw NullPointerException(msg).also {
                        it.initCause(e)
                    }
                }

            } else {
                val sql = "insert into $table (attrs, id) values (cast(? as JSONB), ?)"
                s.run(queryOf(sql, attrs, obj.id).asUpdate)
                obj.id

            }
        }

    } catch (e: Exception) {
        LOG.error("[insert] Unable to insert: $obj", e)
        throw e

    } // insert

    override fun update(query: SimpleQuery): Long = (query as SQLQuery).let { q ->
        try {
            q.asSimpleObjectUpdateQueryAction(table).let {
                s.run(it).toLong()
            }

        } catch (e: Exception) {
            LOG.error("[update] Unable to update: $q", e)
            throw e
        }
    }

}
