package com.redhat.labs.lodestar.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.redhat.labs.lodestar.model.Artifact;
import com.redhat.labs.lodestar.model.Category;
import com.redhat.labs.lodestar.model.Commit;
import com.redhat.labs.lodestar.model.Commit.CommitBuilder;
import com.redhat.labs.lodestar.model.Engagement;
import com.redhat.labs.lodestar.model.EngagementUser;
import com.redhat.labs.lodestar.model.GitlabProject;
import com.redhat.labs.lodestar.model.Hook;
import com.redhat.labs.lodestar.model.HostingEnvironment;
import com.redhat.labs.lodestar.model.Launch;
import com.redhat.labs.lodestar.model.Score;
import com.redhat.labs.lodestar.model.Status;
import com.redhat.labs.lodestar.model.UseCase;

public class MockUtils {

    private static ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
            false);

    private MockUtils() {
        throw new IllegalStateException("Utility Class");
    }

    public static Engagement mockMinimumEngagement(String customerName, String projectName, String uuid) {
        return Engagement.builder().customerName(customerName).projectName(projectName).uuid(uuid).build();
    }

    public static Engagement mockEngagement() {

        Engagement engagement = Engagement.builder().customerName("TestCustomer").projectName("TestProject")
                .description("Test Description").location("Raleigh, NC").startDate("20170501").endDate("20170708")
                .archiveDate("20170930").engagementLeadName("Mister Lead").engagementLeadEmail("mister@lead.com")
                .technicalLeadName("Mister Techlead").technicalLeadEmail("mister@techlead.com")
                .customerContactName("Customer Contact").customerContactEmail("customer@contact.com").build();

        return engagement;

    }

    public static HostingEnvironment mockHostingEnvironment(String environmentName, String ocpSubdomain) {
        return HostingEnvironment.builder().environmentName(environmentName).ocpCloudProviderName("provider1")
                .ocpClusterSize("small").ocpPersistentStorageSize("none").ocpSubDomain(ocpSubdomain).ocpVersion("4.x.x")
                .build();
    }

    public static EngagementUser mockEngagementUser(String email, String firstName, String lastName, String role,
            String uuid, boolean reset) {
        return EngagementUser.builder().email(email).firstName(firstName).lastName(lastName).role(role).uuid(uuid)
                .reset(true).build();
    }

    public static Hook mockHook(String pathWithNamespace, String nameWithNamespace, boolean fileChanged,
            String fileName) {
        return Hook.builder().project(mockGitLabProject(pathWithNamespace, nameWithNamespace))
                .commits(Lists.newArrayList(mockCommit(fileName, fileChanged, null))).build();
    }

    public static Hook mockHook(String pathWithNamespace, String nameWithNamespace, boolean fileChanged,
            String fileName, String message) {
        return Hook.builder().project(mockGitLabProject(pathWithNamespace, nameWithNamespace))
                .commits(Lists.newArrayList(mockCommit(fileName, fileChanged, message))).build();
    }

    public static GitlabProject mockGitLabProject(String pathWithNamespace, String nameWithNamspace) {
        return GitlabProject.builder().pathWithNamespace(pathWithNamespace).nameWithNamespace(nameWithNamspace).build();
    }

    public static Commit mockCommit(String fileName, boolean hasChanged, String message) {
        CommitBuilder builder = Commit.builder();
        if (hasChanged) {
            builder.added(Lists.newArrayList("status.json"));
        }
        String msg = (null == message) ? "message for commit" : "manual_refresh";
        builder.message(msg);
        return builder.build();
    }

    public static Status mockStatus(String status) {
        return Status.builder().status(status).build();
    }

    public static Category mockCategory(String name) {
        return Category.builder().name(name).build();
    }

    public static Artifact mockArtifact(String title, String type, String link) {
        return Artifact.builder().title(title).type(type).linkAddress(link).build();
    }

    public static Engagement cloneEngagement(Engagement e) throws JsonMappingException, JsonProcessingException {
        String json = mapper.writeValueAsString(e);
        return mapper.readValue(json, Engagement.class);
    }

    public static Launch mockLaunch(String dateTime, String launchedBy, String launchedByEmail) {
        return Launch.builder().launchedDateTime(dateTime).launchedBy(launchedBy).launchedByEmail(launchedByEmail)
                .build();
    }

    public static UseCase mockUseCase(String title, String description, Integer order) {
        return UseCase.builder().title(title).description(description).order(order).build();
    }

    public static Score mockScore(String name, Double value) {
        return Score.builder().name(name).value(value).build();
    }
}