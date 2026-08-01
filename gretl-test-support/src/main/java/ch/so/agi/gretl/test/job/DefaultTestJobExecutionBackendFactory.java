package ch.so.agi.gretl.test.job;

import ch.so.agi.gretl.test.docker.ContainerUserResolver;
import ch.so.agi.gretl.test.docker.DockerCli;
import ch.so.agi.gretl.test.execution.RuntimeImageBuildExecutor;
import ch.so.agi.gretl.test.execution.RuntimeImageLifecycleArguments;
import ch.so.agi.gretl.test.runtime.RuntimeImageServiceContainer;
import ch.so.agi.gretl.testkit.ExplicitPluginClasspathBuildExecutor;
import ch.so.agi.gretl.testkit.ExplicitPluginClasspathTestConfiguration;
import ch.so.agi.gretl.testkit.PublishedArtifactBuildExecutor;
import ch.so.agi.gretl.testkit.PublishedArtifactTestConfiguration;

import java.util.Optional;

public final class DefaultTestJobExecutionBackendFactory implements TestJobExecutionBackendFactory {
    private TestJobExecutionBackend serviceBackend;

    @Override
    public synchronized TestJobExecutionBackend create(TestJobExecutionTarget target, TestJobBackendContext context) {
        return switch (target) {
            case PLUGIN_CLASSPATH -> new PluginClasspathJobExecutionBackend(new ExplicitPluginClasspathBuildExecutor(
                    ExplicitPluginClasspathTestConfiguration.fromSystemProperties()));
            case PUBLISHED_ARTIFACT -> new PublishedArtifactJobExecutionBackend(new PublishedArtifactBuildExecutor(
                    PublishedArtifactTestConfiguration.fromSystemProperties()));
            case RUNTIME_IMAGE_ONE_SHOT -> new RuntimeImageOneShotJobExecutionBackend(new RuntimeImageBuildExecutor(
                    context.runtimeImage().orElseThrow(() -> new IllegalArgumentException("Runtime image is required")),
                    new DockerCli(), new ContainerUserResolver(), new RuntimeImageLifecycleArguments()));
            case RUNTIME_IMAGE_SERVICE -> {
                if (serviceBackend == null) {
                    serviceBackend = RuntimeImageServiceJobExecutionBackend.start(
                            context.runtimeImage().orElseThrow(() -> new IllegalArgumentException("Runtime image is required")),
                            context.serviceJobsRoot().orElseThrow(() -> new IllegalArgumentException("serviceJobsRoot is required")),
                            context.serviceGradleHome().orElseThrow(() -> new IllegalArgumentException("serviceGradleHome is required")),
                            context.dockerNetwork(), context.runtimeUser());
                }
                yield serviceBackend;
            }
        };
    }

    @Override
    public synchronized void close() {
        if (serviceBackend != null) {
            serviceBackend.close();
            serviceBackend = null;
        }
    }
}
