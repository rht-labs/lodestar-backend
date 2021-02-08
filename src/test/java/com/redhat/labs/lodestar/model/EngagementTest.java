package com.redhat.labs.lodestar.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import org.apache.commons.compress.utils.Sets;
import org.junit.jupiter.api.Test;

import com.google.common.collect.Lists;

class EngagementTest {

    @Test
    void testDeepCopyWithNoSubCollections() {

        Engagement e = Engagement.builder().build();
        Engagement copy = Engagement.deepCopy(e);

        assertNotSame(e, copy);
        
    }
    
    @Test
    void testDeepCopyWithSubCollections() {

        Engagement e = Engagement.builder().hostingEnvironments(Lists.newArrayList(createHostingEnvironment()))
                .engagementUsers(Sets.newHashSet(createEngagementUser())).commits(Lists.newArrayList(createCommit()))
                .categories(Lists.newArrayList(createCategory())).useCases(Lists.newArrayList(createUseCase()))
                .artifacts(Lists.newArrayList(createArtifact())).build();

        Engagement copy = Engagement.deepCopy(e);

        assertNotSame(e, copy);

        // hosting environments
        assertNotSame(e.getHostingEnvironments(), copy.getHostingEnvironments());
        assertEquals(1, e.getHostingEnvironments().size());
        assertEquals(1, copy.getHostingEnvironments().size());
        assertNotSame(e.getHostingEnvironments().get(0), copy.getHostingEnvironments().get(0));

        // engagement users
        assertNotSame(e.getEngagementUsers(), copy.getEngagementUsers());
        assertEquals(1, e.getEngagementUsers().size());
        assertEquals(1, copy.getEngagementUsers().size());
        assertNotSame(e.getEngagementUsers().iterator().next(), copy.getEngagementUsers().iterator().next());

        // commits
        assertNotSame(e.getCommits(), copy.getCommits());
        assertEquals(1, e.getCommits().size());
        assertEquals(1, copy.getCommits().size());
        assertNotSame(e.getCommits().get(0), copy.getCommits().get(0));

        // categories
        assertNotSame(e.getCategories(), copy.getCategories());
        assertEquals(1, e.getCategories().size());
        assertEquals(1, copy.getCategories().size());
        assertNotSame(e.getCategories().get(0), copy.getCategories().get(0));

        // use case
        assertNotSame(e.getUseCases(), copy.getUseCases());
        assertEquals(1, e.getUseCases().size());
        assertEquals(1, copy.getUseCases().size());
        assertNotSame(e.getUseCases().get(0), copy.getUseCases().get(0));

        // artifacts
        assertNotSame(e.getArtifacts(), copy.getArtifacts());
        assertEquals(1, e.getArtifacts().size());
        assertEquals(1, copy.getArtifacts().size());
        assertNotSame(e.getArtifacts().get(0), copy.getArtifacts().get(0));
        
    }

    HostingEnvironment createHostingEnvironment() {
        return HostingEnvironment.builder().environmentName("some-environment").build();
    }

    EngagementUser createEngagementUser() {
        return EngagementUser.builder().firstName("bob").lastName("smith").build();
    }

    Commit createCommit() {
        return Commit.builder().id("1234").build();
    }

    Category createCategory() {
        return Category.builder().name("cat1").build();
    }

    UseCase createUseCase() {
        return UseCase.builder().id("4321").build();
    }

    Artifact createArtifact() {
        return Artifact.builder().title("art1").build();
    }

}
