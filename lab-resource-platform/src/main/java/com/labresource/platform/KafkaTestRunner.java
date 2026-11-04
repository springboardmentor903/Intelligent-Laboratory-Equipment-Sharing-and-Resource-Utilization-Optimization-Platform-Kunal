
package com.labresource.platform;

import org.springframework.boot.CommandLineRunner;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaTestRunner implements CommandLineRunner {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public KafkaTestRunner(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void run(String... args) {
        kafkaTemplate.send("lab-events", "Kafka connected successfully")
                .whenComplete((result, error) -> {
                    if (error == null) {
                        System.out.println(
                                "Kafka message sent: Kafka connected successfully"
                        );
                    } else {
                        System.out.println(
                                "Kafka send failed: " + error.getMessage()
                        );
                    }
                });
    }
}
