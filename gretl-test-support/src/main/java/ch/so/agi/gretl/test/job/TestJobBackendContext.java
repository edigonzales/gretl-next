package ch.so.agi.gretl.test.job;

import ch.so.agi.gretl.test.runtime.RuntimeImageDescriptor;

import java.net.URI;
import java.nio.file.Path;
import java.util.Optional;

public record TestJobBackendContext(
        Optional<Path> explicitPluginClasspathFile,
        Optional<Path> testKitDirectory,
        Optional<URI> publishedRepository,
        Optional<String> pluginVersion,
        Optional<RuntimeImageDescriptor> runtimeImage,
        Optional<Path> serviceJobsRoot,
        Optional<Path> serviceGradleHome,
        Optional<String> dockerNetwork,
        Optional<String> runtimeUser) {
    public TestJobBackendContext {
        explicitPluginClasspathFile = explicitPluginClasspathFile == null ? Optional.empty() : explicitPluginClasspathFile;
        testKitDirectory = testKitDirectory == null ? Optional.empty() : testKitDirectory;
        publishedRepository = publishedRepository == null ? Optional.empty() : publishedRepository;
        pluginVersion = pluginVersion == null ? Optional.empty() : pluginVersion;
        runtimeImage = runtimeImage == null ? Optional.empty() : runtimeImage;
        serviceJobsRoot = serviceJobsRoot == null ? Optional.empty() : serviceJobsRoot;
        serviceGradleHome = serviceGradleHome == null ? Optional.empty() : serviceGradleHome;
        dockerNetwork = dockerNetwork == null ? Optional.empty() : dockerNetwork;
        runtimeUser = runtimeUser == null ? Optional.empty() : runtimeUser;
    }
}
