package com.example.jobqueue.worker;

import org.springframework.stereotype.Component;

@Component
public class WordCountWorker implements JobWorker {

    @Override
    public String process(String payload) {
        if (payload == null || payload.isBlank()) {
            return "0";
        }
        long count = payload.trim().split("\\s+").length;
        return String.valueOf(count);
    }
}
