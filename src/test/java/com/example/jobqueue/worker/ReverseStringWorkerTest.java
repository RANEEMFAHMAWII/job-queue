package com.example.jobqueue.worker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReverseStringWorkerTest {

    private ReverseStringWorker worker;

    @BeforeEach
    void setUp() {
        worker = new ReverseStringWorker();
    }

    @Test
    void shouldReverseSimpleString() {
        assertEquals("olleh", worker.process("hello"));
    }

    @Test
    void shouldReverseStringWithSpaces() {
        assertEquals("dlrow olleh", worker.process("hello world"));
    }

    @Test
    void shouldReturnEmptyForEmptyString() {
        assertEquals("", worker.process(""));
    }

    @Test
    void shouldReturnEmptyForNull() {
        assertEquals("", worker.process(null));
    }

    @Test
    void shouldHandleSingleCharacter() {
        assertEquals("a", worker.process("a"));
    }

    @Test
    void shouldHandlePalindrome() {
        assertEquals("racecar", worker.process("racecar"));
    }
}
