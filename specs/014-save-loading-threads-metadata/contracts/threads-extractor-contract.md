# Contract: Threads Link Metadata Extractor

## Module: `LinkMetadataExtractor`

### Method Signatures
```kotlin
object LinkMetadataExtractor {
    suspend fun extractMetadata(url: String): LinkMetadata
    fun formatDomainToPlatformName(domain: String): String
    // Internal routing:
    internal suspend fun extractThreadsMetadata(url: String, domain: String): LinkMetadata
}
```

### URL Patterns Handled
1. **Threads Post**:
   - `https://www.threads.net/@{username}/post/{postId}`
   - `https://threads.net/@{username}/post/{postId}`
   - `https://threads.net/@{username}/post/{postId}?xmt=...`
2. **Threads Shortlinks**:
   - `https://www.threads.net/t/{postId}`
   - `https://threads.net/t/{postId}`
3. **Threads User Profile**:
   - `https://www.threads.net/@{username}`
   - `https://threads.net/@{username}`
4. **Threads Root Domain**:
   - `https://threads.net`
   - `https://www.threads.net`

### Output Expectations

| Input URL | Expected `title` | Expected `sourcePlatform` | Expected `faviconUrl` |
|-----------|------------------|---------------------------|-----------------------|
| `https://www.threads.net/@zuck/post/Cx123` | `"Mark Zuckerberg (@zuck) on Threads"` or `"@zuck on Threads"` | `"@Threads"` | `"https://www.google.com/s2/favicons?domain=threads.net&sz=128"` |
| `https://threads.net/@devemdobro` | `"@devemdobro on Threads"` | `"@Threads"` | `"https://www.google.com/s2/favicons?domain=threads.net&sz=128"` |
| `https://threads.net/t/Cx123` | `"Threads Post"` | `"@Threads"` | `"https://www.google.com/s2/favicons?domain=threads.net&sz=128"` |
| `https://threads.net` | `"Threads"` | `"@Threads"` | `"https://www.google.com/s2/favicons?domain=threads.net&sz=128"` |

### Domain Formatter Contract
- `formatDomainToPlatformName("threads.net")` -> `"@Threads"`
- `formatDomainToPlatformName("www.threads.net")` -> `"@Threads"`
