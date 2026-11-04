
package com.labresource.platform;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import java.util.List;

public interface ResourceSearchRepository
        extends ElasticsearchRepository<ResourceSearchDocument, String> {

    List<ResourceSearchDocument> findByNameContaining(String name);

    List<ResourceSearchDocument> findByCategory(String category);

    List<ResourceSearchDocument> findByLocationContaining(String location);
}
