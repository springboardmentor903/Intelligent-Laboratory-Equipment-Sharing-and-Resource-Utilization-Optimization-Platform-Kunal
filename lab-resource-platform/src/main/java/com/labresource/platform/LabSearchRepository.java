
package com.labresource.platform;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

@NoRepositoryBean
public interface LabSearchRepository
        extends ElasticsearchRepository<LabSearchDocument, String> {

    List<LabSearchDocument> findByNameContaining(String name);

    List<LabSearchDocument> findByCategory(String category);
}
