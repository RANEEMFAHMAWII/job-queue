package com.example.jobqueue.worker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WordCountWorkerTest {

    private WordCountWorker worker;

    @BeforeEach
    void setUp() {
        worker = new WordCountWorker();
    }

    @Test
    void shouldCountWordsInSimpleSentence() {
        assertEquals("5", worker.process("the quick brown fox jumps"));
    }

    @Test
    void shouldReturnOneForSingleWord() {
        assertEquals("1", worker.process("hello"));
    }

    @Test
    void shouldHandleMultipleSpaces() {
        assertEquals("3", worker.process("  hello   world   test  "));
    }

    @Test
    void shouldReturnZeroForBlankString() {
        assertEquals("0", worker.process("   "));
    }

    @Test
    void shouldReturnZeroForEmptyString() {
        assertEquals("0", worker.process(""));
    }

    @Test
    void shouldReturnZeroForNull() {
        assertEquals("0", worker.process(null));
    }
}
