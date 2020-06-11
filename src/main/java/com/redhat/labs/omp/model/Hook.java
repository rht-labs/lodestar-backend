package com.redhat.labs.omp.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Hook {

	private String objectKind;
	private String eventName;
	private List<Commit> commits;
	private GitlabProject project;
	
	private static final String regex = "(.*)\\/(.*)\\/(.*)\\/iac";
	
	public boolean didFileChange(String fileName) {
		for(Commit commit : commits) {
			if(commit.didFileChange(fileName)) {
				return true;	
			}
		}
		
		return false;
	}
	
	public String getCustomerName() {
		return project.getPathWithNamespace().replaceAll(regex, "$2");
		
	}
	
	public String getEngagementName() {
		return project.getPathWithNamespace().replaceAll(regex, "$3");
	}
	
}
