package at.reithma

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch

fun kCacheBlocking() {
    val kCache = CachedValue<String>()
    println(kCache.getBlocking())
    kCache.storeBlocking("Hello World!")
    println(kCache.getBlocking())
}

suspend fun kCacheSuspending() {
    val kCache = CachedValue(42)
    println(kCache.get())
    kCache.store(69)
    println(kCache.get())
    kCache.clear()
    println(kCache.get())
}

suspend fun observingKcache(scope: CoroutineScope) {
    val kCacheChar = CachedValue<Char>('A')
    val kCacheList = CachedValue<List<Int>>(listOf(1, 2, 3))
    val obsOnChar = kCacheChar.observe()
    val obsOnList = kCacheList.observe()

    scope.launch {
        obsOnList
            .take(2)
            .collect { println("LIST: $it") }

    }
    scope.launch {
        obsOnChar
            .take(2)
            .collect { println("CHAR: $it") }
    }
    delay(500) // small delay to get the scopes launched before storing, otherwise we would only receive the end values
    kCacheChar.store('B')
    kCacheList.store(listOf(4, 5, 6))
}

suspend fun main() {
    kCacheBlocking()
    println("---------")

    kCacheSuspending()
    println("---------")

    coroutineScope {
        observingKcache(this)
    }
}
