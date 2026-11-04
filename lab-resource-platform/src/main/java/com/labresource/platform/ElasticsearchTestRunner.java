
package com.labresource.platform;

import org.springframework.boot.CommandLineRunner;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Component;

@Component
public class ElasticsearchTestRunner implements CommandLineRunner {

    private final ElasticsearchOperations elasticsearchOperations;

    public ElasticsearchTestRunner(
            ElasticsearchOperations elasticsearchOperations) {
        this.elasticsearchOperations = elasticsearchOperations;
    }

    @Override
    public void run(String... args) {
        System.out.println("=== ELASTICSEARCH TEST STARTED ===");

        try {
            IndexOperations indexOperations =
                    elasticsearchOperations.indexOps(LabSearchDocument.class);

            if (!indexOperations.exists()) {
                boolean created = indexOperations.create();

                if (!created) {
                    throw new IllegalStateException(
                            "Elasticsearch index could not be created"
                    );
                }

                indexOperations.putMapping(
                        indexOperations.createMapping()
                );
            }

            System.out.println("Elasticsearch index ready!");

            LabSearchDocument document = new LabSearchDocument(
                    "test-001",
                    "Dell Workstation",
                    "COMPUTER"
            );

            elasticsearchOperations.save(document);

            System.out.println("Elasticsearch document saved!");

            LabSearchDocument result = elasticsearchOperations.get(
                    "test-001",
                    LabSearchDocument.class
            );

            if (result != null) {
                System.out.println("Elasticsearch document retrieved!");
                System.out.println("Name: " + result.getName());
                System.out.println("Category: " + result.getCategory());
                System.out.println("=== ELASTICSEARCH TEST SUCCESSFUL ===");
            } else {
                System.out.println(
                        "Document was not retrieved. Check the index mapping."
                );
            }

        } catch (Exception e) {
            System.err.println(
                    "Elasticsearch test failed: " + e.getMessage()
            );
            e.printStackTrace();
        }
    }
}

