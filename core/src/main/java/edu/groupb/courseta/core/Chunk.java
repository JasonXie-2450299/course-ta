package edu.groupb.courseta.core;

/** Location is a physical PDF page or an inclusive UTF-8 text line range. */
public record Chunk(String document, String location, String text) { }
