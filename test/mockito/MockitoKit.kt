/**
 * Returns the mock object itself for any method that returns the specified class.
 * See http://jakegoulding.com/blog/2012/01/09/stubbing-builder-pattern-in-mockito/
 */
class AnswerWithSelf(private val clazz: Class<*>) : Answer<Any> {
    private val delegate: Answer<Any> = ReturnsEmptyValues()

    override fun answer(invocation: InvocationOnMock): Any = when (invocation.method.returnType) {
        clazz -> {
            invocation.mock
        }
        else -> {
            delegate.answer(invocation)
        }
    }

}
