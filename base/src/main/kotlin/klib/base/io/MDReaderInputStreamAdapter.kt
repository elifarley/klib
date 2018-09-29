package klib.base.io

import java.io.EOFException
import java.security.DigestInputStream

/**
 * Created by elifarley on 05/12/16.
 */
abstract class MDReaderInputStreamAdapter<out D>(protected val mdi: DigestInputStream) : IOWithDigest.MDReader(mdi.messageDigest) {

    private enum class State { HEADER, DETAIL, TRAILER, DONE}

    private var state: State = State.HEADER

    protected abstract val header: Map<String, Any>
    protected abstract val detailIterable: Iterable<D>

    private var lineCount: Int = 0
    private lateinit var detailIterator: Iterator<D>

    override fun read(): Any? = when (state) {

        State.HEADER -> { state = State.DETAIL; detailIterator = detailIterable.iterator(); lineCount++; header }

        State.DETAIL -> if (detailIterator.hasNext()) { lineCount++; detailIterator.next() }
                        else { state = State.TRAILER; ++lineCount }

        State.TRAILER -> { state = State.DONE; close(); null }

        State.DONE -> throw EOFException()

    }

    override fun close() {
        mdi.close()
    }

}

