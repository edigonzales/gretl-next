package ch.so.agi.gretl.internal.interlis;

import ch.so.agi.gretl.tasks.Ili2pgExport;
import ch.so.agi.gretl.tasks.Ili2pgImport;
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

class TransferInputResolverTest {

    @TempDir
    Path projectDir;

    @Test
    void resolvesSortedLocalFilesFromFileTree() throws Exception {
        Project project = project();
        Files.writeString(projectDir.resolve("B.xtf"), "b", StandardCharsets.UTF_8);
        Files.writeString(projectDir.resolve("A.xtf"), "a", StandardCharsets.UTF_8);
        Ili2pgImport task = project.getTasks().create("importData", Ili2pgImport.class);

        task.transferFiles(project.fileTree(projectDir.toFile()).matching(pattern -> pattern.include("*.xtf")));

        TransferInputResolver.TransferInputs inputs = new TransferInputResolver().resolve(task, true);
        assertEquals(List.of(
                projectDir.resolve("A.xtf").toAbsolutePath().toString(),
                projectDir.resolve("B.xtf").toAbsolutePath().toString()
        ), inputs.localFiles());
    }

    @Test
    void resolvesIlidataIdsFromRepositoryDataIds() {
        Ili2pgImport task = project().getTasks().create("importData", Ili2pgImport.class);

        task.repositoryDataIds("ilidata:ch.so.demo.a", "ilidata:ch.so.demo.b");

        assertEquals(List.of("ilidata:ch.so.demo.a", "ilidata:ch.so.demo.b"),
                new TransferInputResolver().resolve(task, true).repositoryIds());
    }

    @Test
    void resolvesOutputFilesFromDataFiles() throws Exception {
        Project project = project();
        Files.writeString(projectDir.resolve("out.xtf"), "data", StandardCharsets.UTF_8);
        Ili2pgExport task = project.getTasks().create("exportData", Ili2pgExport.class);

        task.dataFiles(projectDir.resolve("out.xtf").toFile());

        assertEquals(List.of(projectDir.resolve("out.xtf").toAbsolutePath().toString()),
                new TransferInputResolver().resolveLocal(task).localFiles());
    }

    @Test
    void rejectsLocalFilesAndRepositoryIdsTogether() throws Exception {
        Project project = project();
        Files.writeString(projectDir.resolve("data.xtf"), "data", StandardCharsets.UTF_8);
        Ili2pgImport task = project.getTasks().create("importData", Ili2pgImport.class);
        task.transferFiles(projectDir.resolve("data.xtf").toFile());
        task.repositoryDataIds("ilidata:ch.so.demo.a");

        GradleException exception = assertThrows(GradleException.class,
                () -> new TransferInputResolver().resolve(task, true));

        assertEquals("Use either transferFiles(...) or repositoryDataIds(...), not both.", exception.getMessage());
    }

    @Test
    void exportTasksDoNotExposeImportFileDsl() {
        assertThrows(NoSuchMethodException.class,
                () -> Ili2pgExport.class.getMethod("repositoryDataIds", String[].class));
        assertThrows(NoSuchMethodException.class,
                () -> Ili2pgExport.class.getMethod("transferFiles", Object[].class));
    }

    private Project project() {
        return ProjectBuilder.builder()
                .withProjectDir(projectDir.toFile())
                .build();
    }
}
