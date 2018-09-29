package klib.base.math

import org.uncommons.maths.random.XORShiftRNG

object RandomKit {

    private val perThreadRandom = ThreadLocal<XORShiftRNG>()

    /**
     * Gets a thread-local <code>Random</code> object. It is
     * stored thread-locally so that simultaneous invocations can have different
     * Random objects.
     */
    val threadLocalXORShiftRNG
        get() = perThreadRandom.get().let {
            it ?: XORShiftRNG().also {
                perThreadRandom.set(it)

            }

        }

}
