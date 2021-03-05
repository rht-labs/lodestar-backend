package com.redhat.labs.lodestar.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.gradle.internal.impldep.com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;

class HookTest {

    @Test
    void testContainsAnyMessageFound() {

        Commit c1 = Commit.builder().message("message1").build();
        Commit c2 = Commit.builder().message("message2").build();
        Commit c3 = Commit.builder().message("message3").build();
        Hook hook = Hook.builder().commits(Lists.newArrayList(c1, c2, c3)).build();

        assertTrue(hook.containsAnyMessage(Lists.newArrayList("message3")));

    }

    @Test
    void testContainsAnyMessageNotFound() {

        Commit c1 = Commit.builder().message("message1").build();
        Commit c2 = Commit.builder().message("message2").build();
        Commit c3 = Commit.builder().message("message3").build();
        Hook hook = Hook.builder().commits(Lists.newArrayList(c1, c2, c3)).build();

        assertFalse(hook.containsAnyMessage(Lists.newArrayList("message5")));

    }

}
