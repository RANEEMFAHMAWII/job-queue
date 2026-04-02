package com.example.jobqueue.worker;

public interface JobWorker {

    String process(String payload);
}
