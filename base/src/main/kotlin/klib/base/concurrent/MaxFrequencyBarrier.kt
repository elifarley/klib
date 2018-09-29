package klib.base.concurrent

interface EventBarrier {
    fun await(itemCount: Int = 1)
}

class MaxFrequencyBarrier
private constructor (itemsPerSecond: Double) : EventBarrier {

    val millisStep: Int

    init {
        millisStep = (1000 / itemsPerSecond).toInt()
    }

    override fun await(itemCount: Int) = await(millisStep, itemCount)

    companion object {

        fun newInstance(itemsPerSecond: Double = 1e0): EventBarrier = MaxFrequencyBarrier(itemsPerSecond)

        var lastMillis = System.currentTimeMillis()

        // TODO Return max items that can be processed
        @Synchronized
        fun await(millisStep: Int, itemCount: Int = 1): Unit {

            val now = System.currentTimeMillis()
            val toSleep = lastMillis - now

            if (toSleep > 0) {
                lastMillis += millisStep * itemCount
                Thread.sleep(toSleep)

            } else {
                lastMillis = now + millisStep * itemCount

            }

        }

    }

}
