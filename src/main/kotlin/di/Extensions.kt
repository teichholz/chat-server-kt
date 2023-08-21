package di

import org.koin.core.component.KoinComponent

fun <A> di(block: context(KoinComponent) () -> A): A {
    return object : KoinComponent {}.run(block)
}

suspend fun <A> dis(block: suspend context(KoinComponent) () -> A): A {
    return object : KoinComponent {
        suspend fun run(): A = block(this)
    }.run()
}
