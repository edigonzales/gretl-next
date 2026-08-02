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

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Set;

public final class DefaultTestJobExecutionBackendFactory implements TestJobExecutionBackendFactory {
    private final EnumMap<TestJobExecutionTarget, TestJobExecutionBackend> backends =
            new EnumMap<>(TestJobExecutionTarget.class);
    private final List<TestJobExecutionTarget> creationOrder = new ArrayList<>();
    private TestJobBackendContext context;

    public DefaultTestJobExecutionBackendFactory() {
    }

    public DefaultTestJobExecutionBackendFactory(TestJobBackendContext context) {
        this.context = context;
    }

    @Override
    public synchronized TestJobExecutionBackend require(TestJobExecutionTarget target) {
        if (target == null) throw new IllegalArgumentException("target must not be null");
        TestJobExecutionBackend existing = backends.get(target);
        if (existing != null) return existing;
        if (context == null) {
            throw new IllegalStateException("A backend context must be configured before requiring " + target);
        }
        TestJobExecutionBackend created = switch (target) {
            case PLUGIN_CLASSPATH -> new PluginClasspathJobExecutionBackend(new ExplicitPluginClasspathBuildExecutor(
                    ExplicitPluginClasspathTestConfiguration.fromSystemProperties()));
            case PUBLISHED_ARTIFACT -> new PublishedArtifactJobExecutionBackend(new PublishedArtifactBuildExecutor(
                    PublishedArtifactTestConfiguration.fromSystemProperties()));
            case RUNTIME_IMAGE_ONE_SHOT -> new RuntimeImageOneShotJobExecutionBackend(new RuntimeImageBuildExecutor(
                    context.runtimeImage().orElseThrow(() -> new IllegalArgumentException("Runtime image is required")),
                    new DockerCli(), new ContainerUserResolver(), new RuntimeImageLifecycleArguments()));
            case RUNTIME_IMAGE_SERVICE -> RuntimeImageServiceJobExecutionBackend.start(
                    context.runtimeImage().orElseThrow(() -> new IllegalArgumentException("Runtime image is required")),
                    context.serviceJobsRoot().orElseThrow(() -> new IllegalArgumentException("serviceJobsRoot is required")),
                    context.serviceGradleHome().orElseThrow(() -> new IllegalArgumentException("serviceGradleHome is required")),
                    context.dockerNetwork(), context.runtimeUser());
        };
        backends.put(target, created);
        creationOrder.add(target);
        return created;
    }

    @Override
    public synchronized TestJobExecutionBackend create(TestJobExecutionTarget target, TestJobBackendContext context) {
        if (this.context == null) this.context = context;
        else if (context != null && this.context != context && !this.context.equals(context)) {
            throw new IllegalStateException("A backend factory cannot be reconfigured after first use");
        }
        return require(target);
    }

    @Override
    public Set<TestJobExecutionTarget> availableTargets() {
        return Set.of(TestJobExecutionTarget.values());
    }

    @Override
    public synchronized void close() {
        RuntimeException failure = null;
        for (int i = creationOrder.size() - 1; i >= 0; i--) {
            TestJobExecutionBackend backend = backends.get(creationOrder.get(i));
            try {
                backend.close();
            } catch (RuntimeException e) {
                if (failure == null) failure = new IllegalStateException("Failed to close execution backends");
                failure.addSuppressed(e);
            }
        }
        backends.clear();
        creationOrder.clear();
        if (failure != null) throw failure;
    }
}
