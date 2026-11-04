
package com.labresource.platform;

import com.labresource.platform.resource.entity.Resource;
import com.labresource.platform.resource.repository.ResourceRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ResourceSearchService {

    private final ResourceRepository resourceRepository;
    private final ResourceSearchRepository resourceSearchRepository;

    public ResourceSearchService(
            ResourceRepository resourceRepository,
            ResourceSearchRepository resourceSearchRepository) {
        this.resourceRepository = resourceRepository;
        this.resourceSearchRepository = resourceSearchRepository;
    }

    public long syncResourcesToElasticsearch() {
        List<Resource> resources = resourceRepository.findAll()
                .stream()
                .filter(resource -> Boolean.TRUE.equals(resource.getIsActive()))
                .toList();

        List<ResourceSearchDocument> documents = resources.stream()
                .map(resource -> new ResourceSearchDocument(
                        String.valueOf(resource.getId()),
                        resource.getName(),
                        resource.getCategory(),
                        resource.getDescription(),
                        resource.getLocation(),
                        resource.getStatus()
                ))
                .toList();

        resourceSearchRepository.saveAll(documents);

        return documents.size();
    }

    public List<ResourceSearchDocument> searchByName(String name) {
        return resourceSearchRepository.findByNameContaining(name);
    }

    public List<ResourceSearchDocument> searchByCategory(String category) {
        return resourceSearchRepository.findByCategory(category);
    }

    public List<ResourceSearchDocument> searchByLocation(String location) {
        return resourceSearchRepository.findByLocationContaining(location);
    }
}
