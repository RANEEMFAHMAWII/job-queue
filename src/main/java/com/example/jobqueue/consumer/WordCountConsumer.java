package com.example.jobqueue.consumer;

import com.example.jobqueue.dto.JobMessage;
import com.example.jobqueue.service.JobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Component
public class WordCountConsumer {

    private static final Logger log = LoggerFactory.getLogger(WordCountConsumer.class);

    private final JobService jobService;

    public WordCountConsumer(JobService jobService) {
        this.jobService = jobService;
    }

    @RetryableTopic(
            attempts = "${app.job.max-retries}",
            backoff = @Backoff(delay = 1000, multiplier = 2),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE
    )
    @KafkaListener(topics = "${app.kafka.topics.word-count}", groupId = "job-processor")
    public void consume(JobMessage message) {
        log.info("Received WORD_COUNT job: {}", message.jobId());
        jobService.processJob(message.jobId(), message.jobType(), message.payload());
    }
}
