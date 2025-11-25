@file:OptIn(ExperimentalCoroutinesApi::class)

package at.reithma

import kotlinx.coroutines.*
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CachedValueTest {

    @Test
    fun `concurrent get calls should retrieve only once`() = runTest {
        // Given
        val cachedValue = CachedValue<String>()
        val retrieveCount = AtomicInteger(0)

        // A retrieve function that takes some time to simulate network/db
        cachedValue.setRetrieveFunction {
            retrieveCount.incrementAndGet()
            delay(100)
            "fetched"
        }

        val jobCount = 50

        // When
        // Launch multiple coroutines concurrently
        val jobs = List(jobCount) {
            async {
                cachedValue.get()
            }
        }

        val results = jobs.awaitAll()

        // Then
        // Verify all got the value
        assertTrue(results.all { it == "fetched" })

        // Verify retrieve was called only once
        assertEquals(1, retrieveCount.get(), "Retrieve should be called exactly once, but was ${retrieveCount.get()}")
        // Make sure that the retrieval is executed asynchronously
        assertEquals(100L, currentTime)
    }

    @Test
    fun `should save data synchronously`() = runTest {
        // Given
        val cachedValue = CachedValue<Int>(0)
        val storageCount = AtomicInteger(0)

        cachedValue.setStorageFunction {
            storageCount.incrementAndGet()
            delay(10)
        }

        val jobCount = 50

        // When
        val jobs = List(jobCount) { i ->
            async {
                cachedValue.store(i + 1)
            }
        }

        jobs.awaitAll()

        // Then
        assertEquals(jobCount, storageCount.get())
        // The final value should be one of the stored values (not 0)
        assertTrue(cachedValue.get()!! > 0)
        // Make sure that the time is correct
        assertEquals(500L, currentTime)
    }

    @Test
    fun `sync should load from retrieve and write to store`() = runTest {
        // Given
        val retrieveCalled = AtomicInteger(0)
        val storeCalled = AtomicInteger(0)
        var storedValue = ""

        val cachedValue = CachedValue<String>(
            retrieveFunction = {
                retrieveCalled.incrementAndGet()
                "synced"
            },
            storageFunction = {
                storeCalled.incrementAndGet()
                storedValue = it
            }
        )

        // Initial state: memory empty
        assertEquals(0, retrieveCalled.get(), "Retrieve should not be called initially")
        assertEquals(0, storeCalled.get(), "Store should not be called initially")
        assertEquals("", storedValue, "Stored value should be empty initially")

        // When
        val result = cachedValue.sync()

        // Then
        assertEquals("synced", result)
        assertEquals("synced", cachedValue.get())
        assertEquals(1, retrieveCalled.get(), "Should retrieve once")
        assertEquals(1, storeCalled.get(), "Should store once")
        assertEquals("synced", storedValue, "Should store the correct value")
    }

    @Test
    fun `sync should push existing memory value to store`() = runTest {
        // Given
        val storeCalled = AtomicInteger(0)

        val cachedValue = CachedValue<String>(
            initialValue = "initial",
            storageFunction = { storeCalled.incrementAndGet() }
        )

        // When
        val result = cachedValue.sync()

        // Then
        assertEquals("initial", result)
        assertEquals(1, storeCalled.get(), "Should store existing value")
    }
}
