package com.redhat.labs.lodestar.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CommitTest {
	private static List<String> CHANGES = new ArrayList<>();
	
	
	@BeforeAll
	static void setUp() {
		CHANGES.add("status.json");
	}
	
	@Test
	void testFileChanges() {
		
		Commit commit = Commit.builder().added(CHANGES).build();
		assertTrue(commit.didFileChange("status.json"));
		
		commit = Commit.builder().modified(CHANGES).build();
		assertTrue(commit.didFileChange("status.json"));
		
		commit = Commit.builder().removed(CHANGES).build();
		assertTrue(commit.didFileChange("status.json"));
		
		commit = Commit.builder().build();
		assertFalse(commit.didFileChange("status.json"));
		
	}

}
