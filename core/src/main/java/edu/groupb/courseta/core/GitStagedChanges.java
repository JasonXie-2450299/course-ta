package edu.groupb.courseta.core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** Read-only Git commands; never stages, commits, or reads unstaged file contents. */
public final class GitStagedChanges {
    public record Snapshot(Path repository, String diff) { }
    private final String executable;
    public GitStagedChanges() { this("git"); }
    public GitStagedChanges(String executable) { this.executable = executable; }

    public Snapshot read(Path directory) throws IOException, InterruptedException {
        if (!Files.isDirectory(directory)) throw new IOException("Choose a directory inside a Git repository.");
        Path root = Path.of(run(directory, List.of("rev-parse", "--show-toplevel"), 16_384).strip());
        String diff = run(root, List.of("diff", "--cached", "--no-ext-diff", "--no-textconv", "--no-color",
            "--binary", "--full-index", "--no-renames", "--ignore-submodules=none", "--submodule=short", "--"), SmartActions.MAX_DIFF * 4);
        if (diff.isBlank()) throw new IOException("No staged changes. Stage the intended files or hunks in Git, then try again.");
        if (diff.length() > SmartActions.MAX_DIFF)
            throw new IOException("The complete staged diff exceeds 120,000 characters. Split it into smaller staged batches; nothing was sent to the model.");
        return new Snapshot(root, diff);
    }

    private String run(Path directory, List<String> args, int maxBytes) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>(List.of(executable, "--no-pager", "-c", "core.quotepath=false", "-C", directory.toString()));
        command.addAll(args);
        ProcessBuilder builder = new ProcessBuilder(command).redirectErrorStream(true);
        // Honor the selected directory even if the IDE was launched from a Git hook/shell.
        for (String key : List.of("GIT_DIR", "GIT_WORK_TREE", "GIT_INDEX_FILE", "GIT_EXTERNAL_DIFF")) builder.environment().remove(key);
        builder.environment().put("GIT_TERMINAL_PROMPT", "0");
        Process process;
        try { process = builder.start(); }
        catch (IOException ex) { throw new IOException("Cannot start Git. Install Git and make git available on PATH, then restart IDEA.", ex); }
        var bytes = new ByteArrayOutputStream();
        var failure = new AtomicReference<IOException>();
        Thread reader = Thread.ofVirtual().start(() -> {
            try (var stream = process.getInputStream()) {
                byte[] buffer = new byte[8192];
                for (int n; (n = stream.read(buffer)) != -1;) {
                    if (bytes.size() + n > maxBytes) {
                        failure.set(new IOException("The staged diff is too large. Split the staged changes into smaller batches; no partial diff was sent."));
                        process.destroyForcibly(); return;
                    }
                    bytes.write(buffer, 0, n);
                }
            } catch (IOException ex) { failure.compareAndSet(null, ex); }
        });
        try {
            if (!process.waitFor(30, TimeUnit.SECONDS)) throw new IOException("Git timed out. Check the repository and try again.");
            reader.join(2000);
            if (reader.isAlive()) throw new IOException("Git output did not finish. Please retry.");
            if (failure.get() != null) throw failure.get();
            if (process.exitValue() != 0) throw new IOException("Cannot read staged changes. Select a valid, accessible Git working tree and resolve any index conflicts.");
            return bytes.toString(StandardCharsets.UTF_8);
        } finally {
            if (process.isAlive()) process.destroyForcibly();
            reader.interrupt();
        }
    }
}
