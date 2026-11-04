
package com.labresource.platform;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/resources/search")
public class ResourceSearchController {

    private final ResourceSearchService resourceSearchService;

    public ResourceSearchController(
            ResourceSearchService resourceSearchService) {
        this.resourceSearchService = resourceSearchService;
    }

    @PostMapping("/sync")
    public ResponseEntity<Map<String, Object>> syncResources() {
        long count =
                resourceSearchService.syncResourcesToElasticsearch();

        return ResponseEntity.ok(Map.of(
                "message", "Resources synced to Elasticsearch",
                "syncedCount", count
        ));
    }

    @GetMapping
    public ResponseEntity<List<ResourceSearchDocument>> searchResources(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String location) {

        if (name != null && !name.isBlank()) {
            return ResponseEntity.ok(
                    resourceSearchService.searchByName(name));
        }

        if (category != null && !category.isBlank()) {
            return ResponseEntity.ok(
                    resourceSearchService.searchByCategory(category));
        }

        if (location != null && !location.isBlank()) {
            return ResponseEntity.ok(
                    resourceSearchService.searchByLocation(location));
        }

        return ResponseEntity.badRequest().build();
    }
}
