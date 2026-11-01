package edu.groupb.courseta.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

class GitStagedChangesTest {
    @TempDir Path temp;
    private Path init() throws Exception {
        Path repo = Files.createDirectory(temp.resolve("仓库 with spaces"));
        git(repo, "init", "-q"); git(repo, "config", "user.name", "Course TA Test");
        git(repo, "config", "user.email", "test@example.invalid"); git(repo, "config", "core.autocrlf", "false");
        return repo;
    }
    private void baseline(Path repo) throws Exception {
        Files.writeString(repo.resolve("A.java"), "BASELINE\n");
        Files.writeString(repo.resolve("old.txt"), "old contents\n");
        git(repo, "add", "."); git(repo, "-c", "commit.gpgsign=false", "commit", "-qm", "test baseline");
    }
    @Test void collectsAllStagedFilesFromNestedDirectoryButExcludesUnstagedAndUntrackedText() throws Exception {
        Path repo = init(); baseline(repo); Path sub = Files.createDirectory(repo.resolve("module"));
        Files.writeString(repo.resolve("A.java"), "STAGED_MARKER\n"); git(repo, "add", "A.java");
        Files.writeString(repo.resolve("A.java"), "UNSTAGED_SECRET\n");
        Files.writeString(sub.resolve("新增.java"), "SECOND_STAGED_FILE\n"); git(repo, "add", "module");
        Files.writeString(repo.resolve("untracked.txt"), "UNTRACKED_SECRET\n");
        String before = git(repo, "diff", "--cached");
        var snapshot = new GitStagedChanges().read(sub);
        assertEquals(repo.toRealPath(), snapshot.repository().toRealPath());
        assertTrue(snapshot.diff().contains("STAGED_MARKER")); assertTrue(snapshot.diff().contains("SECOND_STAGED_FILE"));
        assertTrue(snapshot.diff().contains("新增.java"));
        assertFalse(snapshot.diff().contains("UNSTAGED_SECRET")); assertFalse(snapshot.diff().contains("UNTRACKED_SECRET"));
        assertEquals(before, git(repo, "diff", "--cached"));
    }
    @Test void supportsInitialCommitWithoutHead() throws Exception {
        Path repo = init(); Files.writeString(repo.resolve("first.java"), "INITIAL_CHANGE\n"); git(repo, "add", ".");
        assertTrue(new GitStagedChanges().read(repo).diff().contains("INITIAL_CHANGE"));
    }
    @Test void noStagedChangesHasActionableError() throws Exception {
        Path repo = init(); baseline(repo); Files.writeString(repo.resolve("A.java"), "UNSTAGED\n");
        assertTrue(assertThrows(IOException.class, () -> new GitStagedChanges().read(repo)).getMessage().contains("No staged changes"));
    }
    @Test void nonRepositoryIsRejected() {
        assertThrows(IOException.class, () -> new GitStagedChanges().read(temp));
    }
    @Test void includesDeletionRenameAndBinaryChanges() throws Exception {
        Path repo = init(); baseline(repo);
        git(repo, "mv", "old.txt", "renamed.txt"); Files.delete(repo.resolve("A.java"));
        Files.write(repo.resolve("image.bin"), new byte[]{0, 1, 2, 3, 0, 9}); git(repo, "add", "--all");
        String diff = new GitStagedChanges().read(repo).diff();
        assertTrue(diff.contains("old.txt")); assertTrue(diff.contains("renamed.txt"));
        assertTrue(diff.contains("deleted file mode")); assertTrue(diff.contains("GIT binary patch"));
    }
    @Test void oversizedDiffFailsInsteadOfReturningPartialChanges() throws Exception {
        Path repo = init(); Files.writeString(repo.resolve("large.txt"), "a".repeat(120001)); git(repo, "add", ".");
        assertTrue(assertThrows(IOException.class, () -> new GitStagedChanges().read(repo)).getMessage().contains("120,000"));
    }
    @Test void missingGitExplainsInstallationRequirement() throws Exception {
        Path repo = init();
        assertTrue(assertThrows(IOException.class, () -> new GitStagedChanges("courseta-nonexistent-git-executable").read(repo))
            .getMessage().contains("PATH"));
    }
    private String git(Path dir, String... args) throws Exception {
        var command = new ArrayList<>(List.of("git", "-C", dir.toString())); command.addAll(List.of(args));
        Path log = temp.resolve(UUID.randomUUID() + ".log");
        ProcessBuilder builder = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(log.toFile());
        for (String key : List.of("GIT_DIR", "GIT_WORK_TREE", "GIT_INDEX_FILE")) builder.environment().remove(key);
        var process = builder.start();
        try {
            assertTrue(process.waitFor(30, TimeUnit.SECONDS), "Test git command timed out");
            String output = Files.readString(log, StandardCharsets.UTF_8); assertEquals(0, process.exitValue(), output); return output;
        } finally { if (process.isAlive()) process.destroyForcibly(); }
    }
}
