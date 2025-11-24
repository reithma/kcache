# kcache

Kotlin Coroutine Abstraction Cache Holder Engine, a lightweight, thread-safe cache implementation for Kotlin with support for Read-Through and Write-Through patterns.

## Features

- **Thread Safety**: Handles concurrency automatically using `Mutex`, ensuring safe access across multiple coroutines.
- **Reactive State**: Exposes an `observe()` method that returns a `Flow<T>`, allowing you to react to value changes in real-time.
- **Read-Through & Write-Through**: Seamlessly integrate with external data sources (like Databases, APIs, or Shared Preferences). 
    - If the cache is empty, it can automatically fetch data via `retrieveFunction`.
    - When data is updated, it can automatically persist it via `storageFunction`.
- **Flexible API**: Supports both suspending functions for modern Coroutine-based code and blocking bridges (`getBlocking`, `storeBlocking`) for interoperability.

## Usage

### Basic In-Memory Cache

You can use `CachedValue` as a simple holder for data that needs to be accessed safely.

```kotlin
val cache = CachedValue<String>()

// Store a value
cache.storeBlocking("Hello World")

// Retrieve it
println(cache.getBlocking()) // Output: Hello World
```

For coroutines:

```kotlin
runBlocking {
    val cache = CachedValue(initialValue = 42)
    
    println(cache.get()) // 42
    
    cache.store(100)
    println(cache.get()) // 100
    
    cache.clear()
    println(cache.get()) // null
}
```

### Reactive Observation

You can observe changes to the cached value using Kotlin Flows. The flow emits only non-null values.

```kotlin
val userCache = CachedValue<User>()

scope.launch {
    userCache.observe().collect { user ->
        println("User updated: $user")
    }
}

// Later...
userCache.store(User("Alice")) // Triggers collector
```

### Connecting to External Storage (Read-Through & Write-Through)

`CachedValue` shines when acting as a repository layer that syncs with a backend or local storage.

```kotlin
val configCache = CachedValue<String>(
    // Write-Through: Save to DB/API when store() is called
    storageFunction = { value ->
        api.saveConfig(value)
        println("Persisted $value")
    },
    // Read-Through: Fetch from DB/API if cache is empty on get()
    retrieveFunction = {
        println("Fetching from remote...")
        api.fetchConfig() 
    }
)

// First call triggers retrieveFunction because cache is empty
val config = configCache.get() 

// Subsequent calls return cached memory value instantly
val cachedConfig = configCache.get()

// Updates memory AND calls storageFunction
configCache.store("New Config") 

// Force a re-fetch from external source
configCache.refresh()
```

## API Overview

### Main Class: `CachedValue<T>`

| Method | Description |
|--------|-------------|
| `get()` | Returns value from memory. If missing, attempts to fetch via `retrieveFunction`. |
| `store(value)` | Updates memory and persists via `storageFunction`. |
| `observe()` | Returns a `Flow<T>` that emits updates (skips nulls). |
| `refresh()` | Ignores memory and forces a fetch from `retrieveFunction`. |
| `sync()` | Ensures a value is loaded (if null) and persisted (if present). |
| `clear()` | Clears the in-memory value. |
| `getBlocking()` | Blocking wrapper for `get()`. |
| `storeBlocking(value)` | Blocking wrapper for `store()`. |
