package com.example.jobqueue.worker;

import org.springframework.stereotype.Component;

@Component
public class ReverseStringWorker implements JobWorker {

    @Override
    public String process(String payload) {
        if (payload == null) {
            return "";
        }
        return new StringBuilder(payload).reverse().toString();
    }
}
