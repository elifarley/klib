package klib.base

import java.io.File
import java.io.FileInputStream
import java.util.*

abstract class MockableCall<R> {
    abstract fun call(vararg params: Any?, realCall: () -> R): R?
    inline operator fun invoke(vararg params: Any?, noinline realCall: () -> R): R? = call(params, realCall = realCall)
}

interface MockableCallResult<R> {
    var mockResult: R?
    var mockException: Throwable?
    val callCount: Int
    val callParams: Array<out Any?>
    fun reset()
    fun <E> expectCall(result: R? = null, expectedInitialCallCount: Int = 0, expectedCallCount: Int = 1, exception: Throwable? = null, block: (String) -> E): E
    operator fun <E> invoke(result: R? = null, expectedInitialCallCount: Int = 0, expectedCallCount: Int = 1, exception: Throwable? = null, block: (String) -> E) = expectCall(result, expectedInitialCallCount, expectedCallCount, exception, block)
}

interface CountingValue<T> {
    var value: T?
    val valueIsSet: Boolean
    val readCount: Int
    fun reset()
}

class CountingValueImpl<T> : CountingValue<T> {

    override var readCount = 0
        get() = synchronized(field) {
            field
        }
        private set

    override var valueIsSet = false
        private set

    override var value: T? = null
        get() = synchronized(readCount) {
            readCount++
            field
        }
        set(v) = synchronized(readCount) {
            field = v
            valueIsSet = true
            // readCount = 0
        }

    override fun reset() = synchronized(readCount) {
        value = null
        valueIsSet = false
        readCount = 0
    }

}

open class MockableCallHelperUnit(mockFilePropName: String) : MockableCallHelper<Unit>(mockFilePropName) {
    override val importResultFromMapFun: Map<String, String>.() -> Unit = { Unit }
}

abstract class MockableCallHelper<R>(private val mockFilePropName: String) : MockableCall<R>(), MockableCallResult<R> {

    companion object : WithLogging()

    inline val mockableCall: MockableCall<R> get() = this
    inline val mockableCallResult: MockableCallResult<R> get() = this

    override var callParams: Array<out Any?> = arrayOf()

    private val callParamsAsStr
        get() = callParams.let {
            when {
                it.isEmpty() -> ""
                it.size == 1 -> "Param: ${it.first().toString()}; "
                else -> "Params: [${it.joinToString()}]; "
            }

        }

    private val mockName get() = mockFilePropName.removePrefix("TEST_").removeSuffix("_MOCK_FILE") + "-${callId?.toUpperCase()}"

    private var callId: String? = null

    private val _mockResult = CountingValueImpl<R>()

    override var mockResult: R?
        get() = _mockResult.value
        set(v) {
            _mockResult.value = v
        }

    override val callCount get() = _mockResult.readCount

    override var mockException: Throwable? = null

    override fun reset() {
        callId = null
        callParams = arrayOf()
        _mockResult.reset()
    }

    protected abstract val importResultFromMapFun: Map<String, String>.() -> R

    override fun <E> expectCall(result: R?, expectedInitialCallCount: Int, expectedCallCount: Int, exception: Throwable?, block: (String) -> E) = try {
        if (callId != null) {
            throw IllegalArgumentException("[MOCK: $mockName] Expecting call (NESTED!) [RESULT=${
            result?.toString()?.replace("\"", "")
            }]")
        }
        val callId = Integer.toHexString(rnd())!!.also { callId = it }
        LOG.error("[MOCK: $mockName] Expecting call... [RESULT: ${
        result?.toString()?.replace("\"", "")
        }${exception?.let { "; EXCEPTION: ${exception.simpleMessage}" }.orEmpty()}]")
        if (expectedInitialCallCount != _mockResult.readCount) {
            throw IllegalStateException("[MOCK: $mockName] Expected $expectedInitialCallCount as initial call count, but got ${_mockResult.readCount}")
        }

        if (exception != null) mockException = exception
        else mockResult = result

        block(callId).also { expectCallResult ->
            if (_mockResult.readCount != expectedInitialCallCount + expectedCallCount) {
                throw IllegalStateException("[MOCK: $mockName] Expected call count: $expectedCallCount; actual: ${_mockResult.readCount}")
            }

        }

    } finally {
        reset()
    }

    override fun call(vararg params: Any?, realCall: () -> R): R? =
            mockException?.let { mockResult; throw mockException as Exception } ?: if (_mockResult.valueIsSet) {
                callParams = params.first() as? Array<out Any?> ?: arrayOf()
                mockResult.also {
                    LOG.error("[MOCK #${_mockResult.readCount}: $mockName] ${callParamsAsStr}Returning object: ${
                    it?.toString()?.replace("\"", "")
                    }")
                }

            } else System.getProperty(mockFilePropName, System.getenv(mockFilePropName)).let { mockFilePath ->

                if (mockFilePath.isNullOrEmpty()) {
                    return realCall()
                }

                LOG.error("[MOCK: $mockName] ${callParamsAsStr}Reading mocked response from '$mockFilePath'...")
                importResultFromFile(mockFilePath)
            }

    private fun importResultFromFile(mockFilePath: String?): R = File(mockFilePath).let { mockFile ->
        try {

            if (!mockFile.canRead()) {
                throw IllegalArgumentException("File '${mockFile.canonicalPath}' cannot be read.")
            }

            Properties().let {
                it.load(FileInputStream(mockFile))
                importResultFromMapFun(it as Map<String, String>)
            }

        } catch (e: IllegalArgumentException) {
            throw e

        } catch (e: Exception) {
            throw IllegalArgumentException("Unable to process '${mockFile.path}': $e", e)
        }

    } // File(mockFilePath)

}
