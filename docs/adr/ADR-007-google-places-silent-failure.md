# Post-Mortem: Google Places API Silent Failure & DTO Mapping

## The Issue
After migrating to the New Google Places API (/v1/places:searchText), the Lead Generation Pipeline began silently failing—it would log "Searching Google Places..." and then instantly complete with 0 candidates and 0 errors.

## Root Causes
1. **URL Builder Edge-Case**: UriComponentsBuilder automatically prepends a / if it thinks one is missing. When it encountered :searchText as a path segment, it transformed our URL to /v1/places/:searchText instead of /v1/places:searchText. This caused Google to return a 404 Not Found.
2. **Missing Exception Visibility**: The pipeline caught Exception ex but didn't log the stack trace if it wasn't a recognized error type, meaning the 404 error was completely swallowed.
3. **Strict JSON Deserialization**: Once the URL was fixed, the API returned valid JSON. However, the payload contained extra undocumented fields (e.g., metadata, internal tags). By default, Spring's Jackson2ObjectMapper can fail when mapping JSON to POJOs (Data Transfer Objects) if it encounters unrecognized properties, resulting in a silent failure or List.of() return in our fallback logic.

## The Solution
1. **Explicit URL Concatenation**: Constructed the URL string manually (aseUrl + path) *before* parsing it into UriComponentsBuilder.
2. **Explicit Error Logging**: Added LOG.error(..., ex) inside LeadGenerationPipelineService to ensure any HTTP network failure is visibly logged.
3. **Resilient DTO Mapping**: Decorated the DTO classes (GooglePlacesNewSearchResponse, GooglePlaceNewApiResult) with @JsonIgnoreProperties(ignoreUnknown = true). This guarantees that if Google adds new fields to their API response in the future, our backend won't randomly crash trying to parse them.

## Best Practices & Resources
To prevent these types of mapping and silent failures in the future, consider the following best practices:

- **Always annotate API response DTOs with @JsonIgnoreProperties(ignoreUnknown = true)** when consuming external APIs. External providers frequently change payload schemas.
- **Never swallow exceptions**. Always log at least the x.getMessage() when catching a generic Exception.
- **Use UriComponentsBuilder carefully with custom REST verbs**. Standard REST assumes / demarcated paths; Google's custom :{verb} syntax breaks this assumption.

**Further Reading:**
- [Spring Boot: Jackson Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/howto.html#howto.spring-mvc.customize-jackson-objectmapper)
- [Google Places API: Text Search](https://developers.google.com/maps/documentation/places/web-service/text-search)
- [RFC 3986: URI Generic Syntax](https://datatracker.ietf.org/doc/html/rfc3986)

