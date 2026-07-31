package ch.so.agi.gretl.test.offline;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;

public final class OfflineDependencyClosureVerifier {
    public VerificationReport verify(Path repository, OfflineArtifactManifest manifest,
            Set<ModuleCoordinate> roots) {
        if (!Files.isDirectory(repository)) {
            throw new IllegalArgumentException("Offline Maven repository is missing: " + repository);
        }
        manifest.validate();
        ArrayDeque<ModuleCoordinate> queue = new ArrayDeque<>(roots);
        Set<ModuleCoordinate> visited = new LinkedHashSet<>();
        List<String> edges = new ArrayList<>();
        while (!queue.isEmpty()) {
            ModuleCoordinate coordinate = queue.removeFirst();
            if (!visited.add(coordinate)) continue;
            ResolvedOfflineModule module = readModule(repository, coordinate);
            verifyArtifactFiles(module);
            verifyNoDynamicVersions(module);
            verifyNoExternalFileReferences(module);
            for (ModuleCoordinate dependency : runtimeDependencies(module)) {
                edges.add(coordinate.notation() + " -> " + dependency.notation());
                queue.addLast(dependency);
            }
        }
        return new VerificationReport(List.copyOf(visited), List.copyOf(edges));
    }

    public ResolvedOfflineModule readModule(Path repository, ModuleCoordinate coordinate) {
        Path directory = repository.resolve(coordinate.group().replace('.', '/'))
                .resolve(coordinate.module()).resolve(coordinate.version());
        Path pom = directory.resolve(coordinate.module() + "-" + coordinate.version() + ".pom");
        if (!Files.isRegularFile(pom)) {
            throw new IllegalStateException("Missing POM for offline coordinate " + coordinate.notation());
        }
        try {
            var document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(pom.toFile());
            List<ModuleCoordinate> dependencies = new ArrayList<>();
            var nodes = document.getElementsByTagName("dependency");
            for (int i = 0; i < nodes.getLength(); i++) {
                Element dependency = (Element) nodes.item(i);
                String scope = text(dependency, "scope");
                if (Set.of("test", "provided", "system", "import").contains(scope)) continue;
                dependencies.add(new ModuleCoordinate(text(dependency, "groupId"), text(dependency, "artifactId"),
                        text(dependency, "version")));
            }
            String packaging = text((Element) document.getElementsByTagName("project").item(0), "packaging");
            if (packaging.isBlank()) packaging = "jar";
            Path artifact = directory.resolve(coordinate.module() + "-" + coordinate.version() + ".jar");
            return new ResolvedOfflineModule(coordinate, pom, artifact, packaging, List.copyOf(dependencies));
        } catch (Exception e) {
            throw new IllegalStateException("Cannot parse offline POM " + pom, e);
        }
    }

    public Set<ModuleCoordinate> runtimeDependencies(ResolvedOfflineModule module) {
        return Set.copyOf(module.dependencies());
    }

    public void verifyArtifactFiles(ResolvedOfflineModule module) {
        if (!Files.isRegularFile(module.pom())) {
            throw new IllegalStateException("Missing POM: " + module.pom());
        }
        if ("jar".equals(module.packaging()) && !Files.isRegularFile(module.artifact())) {
            throw new IllegalStateException("Missing JAR for " + module.coordinate().notation() + ": " + module.artifact());
        }
    }

    public void verifyNoDynamicVersions(ResolvedOfflineModule module) {
        if (module.coordinate().version().contains("+") || module.coordinate().version().contains("[")
                || module.coordinate().version().contains("]") || module.coordinate().version().startsWith("latest.")) {
            throw new IllegalStateException("Dynamic offline coordinate: " + module.coordinate().notation());
        }
        module.dependencies().forEach(dependency -> {
            if (dependency.version().contains("+") || dependency.version().contains("[")
                    || dependency.version().contains("]") || dependency.version().startsWith("latest.")) {
                throw new IllegalStateException("Dynamic dependency in " + module.coordinate().notation() + ": "
                        + dependency.notation());
            }
        });
    }

    public void verifyNoExternalFileReferences(ResolvedOfflineModule module) {
        try {
            String text = Files.readString(module.pom());
            if (text.contains("file:") || text.contains("build/classes") || text.contains("build/resources")) {
                throw new IllegalStateException("Host or source reference in " + module.pom());
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + module.pom(), e);
        }
    }

    private static String text(Element parent, String name) {
        var nodes = parent.getElementsByTagName(name);
        return nodes.getLength() == 0 ? "" : nodes.item(0).getTextContent().trim();
    }

    public record ResolvedOfflineModule(ModuleCoordinate coordinate, Path pom, Path artifact,
            String packaging, List<ModuleCoordinate> dependencies) {
    }

    public record VerificationReport(List<ModuleCoordinate> modules, List<String> edges) {
        public String asText() {
            return "modules=" + modules.size() + System.lineSeparator() + String.join(System.lineSeparator(), edges);
        }
    }
}
