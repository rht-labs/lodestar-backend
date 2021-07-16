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
	    List<String> statusFile = new ArrayList<>();
	    statusFile.add("status.json");
		
		Commit commit = Commit.builder().added(CHANGES).build();
		assertTrue(commit.didFileChange(statusFile));
		
		commit = Commit.builder().modified(CHANGES).build();
		assertTrue(commit.didFileChange(statusFile));
		
		commit = Commit.builder().removed(CHANGES).build();
		assertTrue(commit.didFileChange(statusFile));
		
		commit = Commit.builder().build();
		assertFalse(commit.didFileChange(statusFile));
		
	}

}
