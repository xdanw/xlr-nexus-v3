package com.xebialabs.xlrelease.plugin.nexus;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;

import com.xebialabs.deployit.booter.local.LocalBooter;
import com.xebialabs.deployit.plugin.api.reflect.Type;
import com.xebialabs.deployit.plugin.api.udm.ConfigurationItem;
import com.xebialabs.deployit.util.DeployitKeys;
import com.xebialabs.deployit.util.PasswordEncrypter;
import com.xebialabs.xlrelease.actors.triggers.TriggerExecutionSupport;
import com.xebialabs.xlrelease.domain.ReleaseTrigger;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static com.xebialabs.xlrelease.builder.ReleaseTriggerBuilder.newReleaseTrigger;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class PublishedArtifactTest extends TriggerTest {
    private ConfigurationItem nexusServer;

    @BeforeClass
    public static void boot() {
        LocalBooter.bootWithoutGlobalContext();
    }

    @ClassRule
    public static WireMockClassRule wireMockRule = new WireMockClassRule(options().dynamicPort());

    @Rule
    public WireMockClassRule instanceRule = wireMockRule;

    @Before
    public void setUp() {
        super.initMocks();
        PasswordEncrypter.init(DeployitKeys.getPasswordEncryptionKey(""));
        nexusServer = nexusServer();
    }

    private String load(String resourcePath) {
        Resource resource = new ClassPathResource(resourcePath);
        try (InputStream is = resource.getInputStream()) {
            return StreamUtils.copyToString(is, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Resource should be loaded.", e);
        }
    }

    @Test
    public void should_fetch_version_when_artifact_is_release() {
        // arrange
        ReleaseTrigger trigger = newReleaseTrigger("nexus.PublishedArtifact")
                .withIdAndTitle("id")
                .withProperty("repositoryId", "central-proxy")
                .withProperty("groupId", "log4j")
                .withProperty("artifactId", "log4j")
                .withProperty("version", "LATEST")
                .withProperty("server", nexusServer)
                .withTriggerState("")
                .build();


        stubFor(get(urlPathEqualTo("/service/local/artifact/maven/resolve"))
                .withQueryParam("r", equalTo("central-proxy"))
                .withQueryParam("g", equalTo("log4j"))
                .withQueryParam("a", equalTo("log4j"))
                .withQueryParam("v", equalTo("LATEST"))
                .willReturn(
                        aResponse().withBody(load("restito/artifact_maven_resolve_release.json"))
                )
        );

        // act
        TriggerExecutionSupport.ReleaseTriggerResults results = jythonScriptService.executeTrigger(trigger);

        // assert
        assertThat(results.state()).isEqualTo("1.2.17");
        assertThat(results.variable("artifactVersion")).isEqualTo("1.2.17");
        assertThat(results.variable("artifactBaseVersion")).isEqualTo("1.2.17");
        assertThat(results.variable("artifactSnapshotBuildNumber")).isEqualTo("");
        assertThat(results.variable("artifactRepositoryPath")).isEqualTo("/log4j/log4j/1.2.17/log4j-1.2.17.jar");
    }

    @Test
    public void should_fetch_version_when_artifact_is_snapshot() {
        // arrange
        ReleaseTrigger trigger = newReleaseTrigger("nexus.PublishedArtifact")
                .withIdAndTitle("id")
                .withProperty("repositoryId", "apache-snapshots")
                .withProperty("groupId", "org.apache.isis.core")
                .withProperty("artifactId", "runtime")
                .withProperty("version", "0.3.1-SNAPSHOT")
                .withProperty("server", nexusServer)
                .withProperty("classifier", "")
                .withProperty("packaging", "jar")
                .withTriggerState("")
                .build();

        stubFor(get(urlPathEqualTo("/service/local/artifact/maven/resolve"))
                .withQueryParam("r", equalTo("apache-snapshots"))
                .withQueryParam("g", equalTo("org.apache.isis.core"))
                .withQueryParam("a", equalTo("runtime"))
                .withQueryParam("v", equalTo("0.3.1-SNAPSHOT"))
                .withQueryParam("p", equalTo("jar"))
                .willReturn(
                        aResponse().withBody(load("restito/artifact_maven_resolve_snapshot.json"))
                )
        );

        // act
        TriggerExecutionSupport.ReleaseTriggerResults results = jythonScriptService.executeTrigger(trigger);

        // assert
        assertThat(results.state()).isEqualTo("0.3.1-20121203.142748-1");
        assertThat(results.variable("artifactVersion")).isEqualTo("0.3.1-20121203.142748-1");
        assertThat(results.variable("artifactBaseVersion")).isEqualTo("0.3.1-SNAPSHOT");
        assertThat(results.variable("artifactSnapshotBuildNumber")).isEqualTo("1");
        assertThat(results.variable("artifactRepositoryPath")).isEqualTo("/org/apache/isis/core/runtime/0.3.1-SNAPSHOT/runtime-0.3.1-20121203.142748-1.jar");
    }

    @Test(expected = RuntimeException.class)
    public void should_exit_with_error_when_nok_reponse() {
        ReleaseTrigger trigger = newReleaseTrigger("nexus.PublishedArtifact")
                .withIdAndTitle("id")
                .withProperty("server", nexusServer)
                .withTriggerState("")
                .build();
        stubFor(get(urlPathEqualTo("/service/local/artifact/maven/resolve")).willReturn(
                aResponse().withStatus(400)
        ));

        // act
        jythonScriptService.executeTrigger(trigger);
    }

    @Test
    public void should_ignore_not_found_errors_and_init_state_when_trigger_on_initial_publish() {
        ReleaseTrigger trigger = newReleaseTrigger("nexus.PublishedArtifact")
                .withIdAndTitle("id")
                .withProperty("server", nexusServer)
                .withProperty("triggerOnInitialPublish", true)
                .withTriggerState("")
                .build();

        stubFor(get(urlPathEqualTo("/service/local/artifact/maven/resolve")).willReturn(
                aResponse().withStatus(404)
        ));

        // act
        TriggerExecutionSupport.ReleaseTriggerResults results = jythonScriptService.executeTrigger(trigger);

        // assert
        assertThat(results.variable("artifactVersion")).isEqualTo("0.0.0");
        assertThat(results.state()).isEqualTo("0.0.0");
    }


    private ConfigurationItem nexusServer() {
        ConfigurationItem server = Type.valueOf("nexus.Server").getDescriptor().newInstance("test-server");
        server.setProperty("url", "http://localhost:" + wireMockRule.port());
        return server;
    }
}
