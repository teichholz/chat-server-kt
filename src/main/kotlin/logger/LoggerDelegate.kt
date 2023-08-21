package logger

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import kotlin.reflect.full.companionObject

interface HasLogger {
    val logger: Logger
}

class LoggerDelegate<in R : Any> : ReadOnlyProperty<R, Logger> {
    override fun getValue(thisRef: R, property: KProperty<*>) = getLogger(getClassForLogging(thisRef.javaClass))
}

inline fun <R> logging(forClass: Class<*>, block: context(HasLogger) () -> R): R =
    object : HasLogger {
        override val logger: Logger = getLogger(forClass)
    }.run(block)

fun <R> logging(name: String, block: context(HasLogger) () -> R): R =
    object : HasLogger {
        override val logger: Logger = getLogger(name)
    }.run(block)

inline fun <R> logging(block: context(HasLogger) () -> R): R =
    object : HasLogger {
        override val logger: Logger = getLogger(this.javaClass.enclosingClass)
    }.run(block)

fun getLogger(name: String): Logger =
    LoggerFactory.getLogger(name)

fun getLogger(forClass: Class<*>): Logger =
    LoggerFactory.getLogger(forClass)

fun <T : Any> getClassForLogging(javaClass: Class<T>): Class<*> {
    return javaClass.enclosingClass?.takeIf {
        it.kotlin.companionObject?.java == javaClass
    } ?: javaClass
}