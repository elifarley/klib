package klib.db

import java.sql.ResultSet

/**
 * Returns the current row as a map
 */
fun ResultSet.toMap() = HashMap<String, Any?>(metaData.columnCount).let { result ->
    val getColumnName = metaData::getColumnName
    for (c in 1..metaData.columnCount) {
        result[getColumnName(c)] = this.getObject(c)
    }
    result.toMap()
}
