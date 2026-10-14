package com.lecture.enrollment.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Value("${kafka.topic.enrollment-completed}")
    private String enrollmentCompletedTopic;

    @Bean
    public NewTopic enrollmentCompletedTopic() {
        return TopicBuilder.name(enrollmentCompletedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
