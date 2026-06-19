package ch.so.agi.gretl.internal.ili2db;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class Ili2dbDatasetResolverTest {

    @TempDir
    Path projectDir;

    @Test
    void resolvesLocalFilesAndDatasetNamesFromFileCollections() throws Exception {
        Project project = project();
        Files.writeString(projectDir.resolve("B_Dataset.xtf"), "b", StandardCharsets.UTF_8);
        Files.writeString(projectDir.resolve("A_Dataset.xtf"), "a", StandardCharsets.UTF_8);

        var fileTree = project.fileTree(projectDir.toFile()).matching(pattern -> pattern.include("*.xtf"));

        assertEquals(List.of(
                projectDir.resolve("A_Dataset.xtf").toString(),
                projectDir.resolve("B_Dataset.xtf").toString()
        ), Ili2dbDatasetResolver.resolveDataFiles(project, fileTree).files());
        assertEquals(List.of("A", "B"), Ili2dbDatasetResolver.resolveDatasets(fileTree, List.of(0, 1)));
    }

    @Test
    void resolvesIlidataIdentifiers() {
        Project project = project();

        assertEquals(List.of("ilidata:ch.so.demo.a"),
                Ili2dbDatasetResolver.resolveDataFiles(project, "ilidata:ch.so.demo.a").identifiers());
        assertEquals(List.of("ilidata:ch.so.demo.a", "ilidata:ch.so.demo.b"),
                Ili2dbDatasetResolver.resolveDataFiles(project,
                        List.of("ilidata:ch.so.demo.a", "ilidata:ch.so.demo.b")).identifiers());
    }

    @Test
    void rejectsLocalFilesInPlainLists() {
        Project project = project();

        GradleException exception = assertThrows(GradleException.class,
                () -> Ili2dbDatasetResolver.resolveDataFiles(project, List.of("data/a.xtf")));

        assertEquals("dataFile list entries must be ilidata: IDs. Use files(...) or fileTree(...) for local files.",
                exception.getMessage());
    }

    @Test
    void rejectsDatasetCountMismatch() {
        GradleException exception = assertThrows(GradleException.class,
                () -> Ili2dbDatasetResolver.pairFilesAndDatasets(
                        List.of("a.xtf", "b.xtf"), List.of("DatasetA")));

        assertEquals("number of dataset names (1) doesn't match number of files (2)",
                exception.getMessage());
    }

    @Test
    void rejectsInvalidDatasetSubstring() {
        GradleException exception = assertThrows(GradleException.class,
                () -> Ili2dbDatasetResolver.resolveDatasets(List.of("A"), List.of(0, 4)));

        assertEquals("datasetSubstring [0, 4] is outside dataset value 'A'", exception.getMessage());
    }

    private Project project() {
        return ProjectBuilder.builder()
                .withProjectDir(projectDir.toFile())
                .build();
    }
}
