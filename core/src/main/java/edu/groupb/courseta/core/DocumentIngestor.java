package edu.groupb.courseta.core;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Page-preserving PDF and line-preserving text extraction. Never crosses a PDF page. */
public final class DocumentIngestor {
    public static final int CHUNK_CHARS = 1400, OVERLAP_CHARS = 200;
    public record ImportResult(int filesRead, int pagesRead, List<Chunk> chunks, List<String> warnings) { }

    public ImportResult read(Path root, Runnable checkCancelled) throws IOException {
        root = root.toAbsolutePath().normalize();
        List<Path> files;
        Path documentBase;
        if (Files.isDirectory(root)) {
            documentBase = root;
            try (var stream = Files.walk(root)) {
                files = stream.filter(Files::isRegularFile).filter(this::supported).sorted().toList();
            }
        } else if (Files.isRegularFile(root)) {
            if (!supported(root)) throw new IOException("Select a PDF, TXT or Markdown file, or a course-material folder.");
            documentBase = root.getParent();
            files = List.of(root);
        } else {
            throw new IOException("Select an existing PDF, TXT or Markdown file, or a course-material folder.");
        }
        List<Chunk> chunks = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        int read = 0, pages = 0;
        for (Path file : files) {
            checkCancelled.run();
            String name = documentBase.relativize(file).toString().replace('\\', '/');
            List<Chunk> extracted = new ArrayList<>();
            int filePages = 0;
            try {
                if (Files.size(file) > 50L * 1024 * 1024) throw new IOException("file exceeds 50 MiB limit");
                if (file.toString().toLowerCase(Locale.ROOT).endsWith(".pdf")) {
                    try (var pdf = Loader.loadPDF(file.toFile())) {
                        var stripper = new PDFTextStripper();
                        stripper.setSortByPosition(true);
                        for (int p = 1; p <= pdf.getNumberOfPages(); p++) {
                            checkCancelled.run();
                            stripper.setStartPage(p); stripper.setEndPage(p);
                            String text = stripper.getText(pdf).replace('\u0000', ' ').strip();
                            if (text.isBlank()) warnings.add(name + " p. " + p + ": no extractable text; OCR is not supported.");
                            splitPage(name, "p. " + p, text, extracted);
                        }
                        filePages = pdf.getNumberOfPages();
                    }
                } else {
                    List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
                    for (int start = 0; start < lines.size();) {
                        int end = start;
                        StringBuilder text = new StringBuilder();
                        while (end < lines.size() && (text.length() < CHUNK_CHARS || end == start)) {
                            text.append(lines.get(end++)).append('\n');
                        }
                        splitPage(name, "lines " + (start + 1) + "-" + end, text.toString(), extracted);
                        start = end < lines.size() && end - start > 2 ? end - 2 : end;
                    }
                }
                chunks.addAll(extracted); pages += filePages; read++;
            } catch (IOException | IllegalArgumentException ex) {
                warnings.add(name + ": skipped (" + ex.getClass().getSimpleName() + "). Check encoding, permissions or PDF integrity.");
            }
        }
        if (files.isEmpty()) warnings.add("No PDF, TXT or Markdown files were found.");
        return new ImportResult(read, pages, List.copyOf(chunks), List.copyOf(warnings));
    }

    private boolean supported(Path p) {
        String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
        return n.endsWith(".pdf") || n.endsWith(".txt") || n.endsWith(".md");
    }

    static void splitPage(String file, String location, String text, List<Chunk> out) {
        for (int start = 0; start < text.length();) {
            int end = Math.min(text.length(), start + CHUNK_CHARS);
            if (end < text.length()) {
                int newline = text.lastIndexOf('\n', end);
                if (newline > start + CHUNK_CHARS / 2) end = newline;
            }
            String part = text.substring(start, end).strip();
            if (!part.isBlank()) out.add(new Chunk(file, location, part));
            if (end == text.length()) break;
            start = Math.max(start + 1, end - OVERLAP_CHARS);
        }
    }
}
