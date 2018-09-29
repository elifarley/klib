package klib.db

import klib.json.SimpleObject
/*
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.Job
import java.util.stream.Stream
*/

interface SimpleRepository<T, PK> {
    operator fun get(id: PK) = find(id)
    fun exists(id: PK): Boolean
    fun find(id: PK): T?
    fun findAll(query: SimpleQuery? = null): Sequence<T>
    fun insert(obj: T): PK
    fun update(query: SimpleQuery): Long
    fun delete(id: PK): Boolean
    fun deleteAll(query: SimpleQuery): Long
}

/*
interface SimpleRepositoryAsync<T> {
    fun find(id: String): Deferred<T>
    fun findAll(query: SimpleQuery): Deferred<Stream<T>>
    fun insert(obj: T)
    fun delete(id: String): Job
    fun deleteAll(query: SimpleQuery): Deferred<Long>
}
*/

sealed class SimpleQuery

abstract class FilterQuery : SimpleQuery() {
    abstract val predicate: (SimpleObject<*>) -> Boolean
}

open class SQLQuery(val isSimpleSelect: Boolean = false, val sql: String, vararg vparams: Any?) : SimpleQuery() {
    val params: Array<out Any?> = vparams
    val sqlTemplate: String? get() = if (isSimpleSelect) "select * from %s where $sql" else null
    companion object {
        fun byField(field: String, value: Any?) = SQLQuery(
                false, "$field = ?", value
        )
    }
}

class ByFieldFilterQuery(field: String, value: Any?): FilterQuery() {
    override val predicate: (SimpleObject<*>) -> Boolean = {
        it[field] == value
    }
}
