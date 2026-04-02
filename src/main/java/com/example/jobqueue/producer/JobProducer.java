package com.example.jobqueue.producer;

import com.example.jobqueue.dto.JobMessage;
import com.example.jobqueue.entity.JobType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class JobProducer {

    private static final Logger log = LoggerFactory.getLogger(JobProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String wordCountTopic;
    private final String reverseStringTopic;

    public JobProducer(KafkaTemplate<String, Object> kafkaTemplate,
                       @Value("${app.kafka.topics.word-count}") String wordCountTopic,
                       @Value("${app.kafka.topics.reverse-string}") String reverseStringTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.wordCountTopic = wordCountTopic;
        this.reverseStringTopic = reverseStringTopic;
    }

    public void send(JobMessage message) {
        String topic = resolveTopic(message.jobType());
        log.info("Publishing job {} to topic {}", message.jobId(), topic);
        kafkaTemplate.send(topic, message.jobId().toString(), message);
    }

    private String resolveTopic(JobType type) {
        return switch (type) {
            case WORD_COUNT -> wordCountTopic;
            case REVERSE_STRING -> reverseStringTopic;
        };
    }
}
