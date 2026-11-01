package edu.groupb.courseta.core;

import com.google.gson.GsonBuilder;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/** Reads the full local corpus and writes reproducible counts, hashes and retrieval evidence. */
public final class CourseAudit {
    public static void main(String[] args) throws Exception {
        Path folder=Path.of(args[0]), output=Path.of(args[1]); Files.createDirectories(output);
        long begin=System.nanoTime();
        var result=new DocumentIngestor().read(folder,()->{});
        var index=new Bm25Index(result.chunks());
        long elapsed=(System.nanoTime()-begin)/1_000_000;
        IndexStore.save(output.resolve("course-index.json"),result.chunks());
        var hashes=new TreeMap<String,String>();
        try(var paths=Files.walk(folder)) {
            for(Path file:paths.filter(Files::isRegularFile).sorted().toList()) {
                var md=MessageDigest.getInstance("SHA-256");
                try(var stream=Files.newInputStream(file)) {byte[] buffer=new byte[65536];for(int n;(n=stream.read(buffer))!=-1;)md.update(buffer,0,n);}
                hashes.put(folder.relativize(file).toString(),HexFormat.of().formatHex(md.digest()));
            }
        }
        List<Object> retrieval=new ArrayList<>();
        for(String q:List.of("What is polymorphism?","How do checked exceptions work?","What is retrieval augmented generation?","Explain constructors","什么是多态","astronomy galaxy telescope")) {
            retrieval.add(Map.of("question",q,"hits",index.search(q,4)));
        }
        var report=Map.of("filesRead",result.filesRead(),"pdfPages",result.pagesRead(),"chunks",result.chunks().size(),
            "importAndIndexMillis",elapsed,"warnings",result.warnings(),"sha256",hashes,"queries",retrieval);
        Files.writeString(output.resolve("course-audit.json"),new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(report));
        System.out.printf("Corpus audit: %d files, %d PDF pages, %d chunks, %d warnings, %d ms%n",result.filesRead(),result.pagesRead(),result.chunks().size(),result.warnings().size(),elapsed);
        for(String q:List.of("polymorphism","checked exceptions","retrieval augmented generation","astronomy galaxy telescope"))
            System.out.println(q+": "+index.search(q,2).stream().map(h->h.chunk().document()+" "+h.chunk().location()).toList());
    }
}
