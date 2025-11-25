package at.reithma

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Thread-safe cache with support for Read-Through and Write-Through patterns.
 *
 * @param T The type of the cached value. Must be non-nullable.
 * @param initialValue Optional initial value.
 * @param retrieveFunction Function to fetch value if missing.
 * @param storageFunction Function to persist value on updates.
 */
class CachedValue<T: Any>(
    initialValue: T? = null,
    private var retrieveFunction: (suspend () -> T?)? = null,
    private var storageFunction: (suspend (T) -> Unit)? = null,
) {
    private val cacheMutex = Mutex()
    private val state = MutableStateFlow(initialValue)

    /**
     * Observes the cached value, emitting only non-nulls.
     */
    fun observe(): Flow<T> = state.filterNotNull()

    /**
     * Returns the value from memory, or fetches it via [retrieveFunction] if missing.
     */
    suspend fun get(): T? {
        // Fast path: check memory first without lock
        state.value?.let { return it }

        cacheMutex.withLock {
            // Double-check: verify if another thread populated it while we waited
            state.value?.let { return it }

            val retrievedResult: T = retrieveFunction?.invoke() ?: return null
            state.value = retrievedResult
            return retrievedResult
        }
    }

    /**
     * Updates memory and persists the value via [storageFunction].
     */
    suspend fun store(value: T) {
        cacheMutex.withLock {
            storageFunction?.invoke(value)
            state.update { value }
        }
    }

    /**
     * Fetches fresh data via [retrieveFunction], ignoring memory cache.
     */
    suspend fun refresh(): T? {
        cacheMutex.withLock {
            val retrievedResult: T = retrieveFunction?.invoke() ?: return null
            state.value = retrievedResult
            return retrievedResult
        }
    }

    /**
     * Ensures the value exists (fetches if needed) and is stored (writes if needed).
     */
    suspend fun sync(): T? {
        cacheMutex.withLock {
            var v = state.value
            if (v == null) {
                v = retrieveFunction?.invoke()
                if (v != null) {
                    state.value = v
                }
            }

            if (v != null) {
                storageFunction?.invoke(v)
            }
            return v
        }
    }

    /**
     * Clears the in-memory cache.
     */
    suspend fun clear() = cacheMutex.withLock { state.value = null }

    /**
     * Blocking version of [get].
     */
    fun getBlocking(): T? = runBlocking { get() }

    /**
     * Blocking version of [store].
     */
    fun storeBlocking(value: T) = runBlocking { store(value) }

    /**
     * Sets the storage persistence function.
     */
    fun setStorageFunction(function: suspend (T) -> Unit) {
        storageFunction = function
    }

    /**
     * Sets the external retrieval function.
     */
    fun setRetrieveFunction(function: suspend () -> T?) {
        retrieveFunction = function
    }
}
