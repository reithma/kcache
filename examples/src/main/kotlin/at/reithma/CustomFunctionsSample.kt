package at.reithma

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch

suspend fun observeCustomStorageFunctions(scope: CoroutineScope) {
    val kCache1 = CachedValue<Char>(
        storageFunction = {
            println("simulating networkCall")
            delay(2000)
            println("Storing value '$it' in remote API...")
        },
        retrieveFunction = {
            println("------RETRIEVE FUNCTION-----------")
            println("simulating databaseCall")
            delay(1000)
            println("Retrieving value from database...")
            'B'
        }
    )

    val obsOn1 = kCache1.observe()

    scope.launch {
        obsOn1
            .take(2)
            .collect { println("3: $it") }
    }
    delay(500) // small delay to get the scopes launched before storing, otherwise we would only receive the end values
    println("Get kCache1: ${kCache1.get()}")
    println("Get kCache1: ${kCache1.get()}")
    kCache1.store('A')
    kCache1.store('C')
    println("Currently cached values: ${kCache1.get()}")
    println("COMMENCING REFRESH")
    kCache1.refresh()
    println("Currently cached values: ${kCache1.get()}")
}

suspend fun main() {
    coroutineScope {
        observeCustomStorageFunctions(this)
    }
}
