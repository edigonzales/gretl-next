package ch.so.agi.gretl.internal.interlis;

import ch.so.agi.gretl.tasks.AbstractIli2DbTask;
import ch.so.agi.gretl.tasks.Ili2pgImport;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DatasetNameResolverTest {

    @TempDir
    Path projectDir;

    @Test
    void resolvesLegacyDatasetNamesFromFileCollectionAndSubstring() throws Exception {
        Project project = project();
        Files.writeString(projectDir.resolve("B_Dataset.xtf"), "b", StandardCharsets.UTF_8);
        Files.writeString(projectDir.resolve("A_Dataset.xtf"), "a", StandardCharsets.UTF_8);
        Ili2pgImport task = project.getTasks().create("importData", Ili2pgImport.class);

        task.dataset(project.fileTree(projectDir.toFile()).matching(pattern -> pattern.include("*.xtf")));
        task.datasetSubstring(List.of(0, 1));

        assertEquals(List.of("A", "B"), new DatasetNameResolver().resolve(task));
    }

    @Test
    void preservesLegacyDatasetSubstringEndIndexSemantics() {
        Ili2pgImport task = project().getTasks().create("importData", Ili2pgImport.class);

        task.dataset(List.of("A_Dataset", "B_Dataset"));
        task.datasetSubstring(List.of(0, 1, 2, 3, 4));

        assertEquals(List.of("A_Da", "B_Da"), new DatasetNameResolver().resolve(task));
    }

    @Test
    void derivesDatasetNamesFromTransferFilesAndSlice() throws Exception {
        Project project = project();
        Files.createDirectories(projectDir.resolve("transfer"));
        Files.writeString(projectDir.resolve("transfer/prefix_alpha.xtf"), "a", StandardCharsets.UTF_8);
        Ili2pgImport task = project.getTasks().create("importData", Ili2pgImport.class);
        task.transferFiles(projectDir.resolve("transfer/prefix_alpha.xtf").toFile());
        task.datasetNamesFromTransferFiles();
        task.datasetNameSlice(7);

        TransferInputResolver.TransferInputs inputs = new TransferInputResolver().resolve(task, true);
        assertEquals(List.of("alpha"), new DatasetNameResolver().resolve(task, inputs));
    }

    @Test
    void rejectsDatasetCountMismatch() throws Exception {
        Project project = project();
        Files.writeString(projectDir.resolve("a.xtf"), "a", StandardCharsets.UTF_8);
        Files.writeString(projectDir.resolve("b.xtf"), "b", StandardCharsets.UTF_8);
        Ili2pgImport task = project.getTasks().create("importData", Ili2pgImport.class);
        task.transferFiles(project.files(projectDir.resolve("a.xtf").toFile(), projectDir.resolve("b.xtf").toFile()));
        task.datasetNames("DatasetA");

        TransferInputResolver.TransferInputs inputs = new TransferInputResolver().resolve(task, true);
        GradleException exception = assertThrows(GradleException.class,
                () -> new DatasetNameResolver().resolve(task, inputs));

        assertEquals("number of dataset names (1) doesn't match number of files (2)", exception.getMessage());
    }

    @Test
    void rejectsInvalidLegacyDatasetSubstring() {
        Ili2pgImport task = project().getTasks().create("importData", Ili2pgImport.class);
        task.dataset(List.of("A"));
        task.datasetSubstring(List.of(0, 4));

        GradleException exception = assertThrows(GradleException.class,
                () -> new DatasetNameResolver().resolve(task));

        assertEquals("datasetSubstring [0, 4] is outside dataset value 'A'", exception.getMessage());
    }

    @Test
    void keepsDatabasePasswordOutOfTaskInputs() throws Exception {
        assertTrue(AbstractIli2DbTask.class.getMethod("getJdbcUrl").isAnnotationPresent(Input.class));
        assertTrue(AbstractIli2DbTask.class.getMethod("getPassword").isAnnotationPresent(Internal.class));
    }

    private Project project() {
        return ProjectBuilder.builder()
                .withProjectDir(projectDir.toFile())
                .build();
    }
}
