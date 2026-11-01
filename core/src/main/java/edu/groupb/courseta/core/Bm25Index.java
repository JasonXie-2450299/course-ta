package edu.groupb.courseta.core;

import java.util.*;
import java.util.regex.Pattern;

/** Local lexical retrieval with an in-memory inverted index and conservative relevance gates. */
public final class Bm25Index {
    public record Hit(Chunk chunk, double score) { }
    private record Posting(int id, int count) { }
    private static final Pattern TOKEN = Pattern.compile("[\\p{L}\\p{N}_]+", Pattern.UNICODE_CHARACTER_CLASS);
    private static final Set<String> STOP = Set.of("a","an","the","is","are","was","were","in","on","at","to","of","for","and","or","with","by","as","it","this","that","what","how","why","does","do","can","could","please","explain","me","my","about","according","course","lecture","slides","java","code","from","be","when","which","your","you","use","using","between","work","mean","would","should","there","their","these","those");
    private static final Map<String,String> ALIASES = Map.ofEntries(
        Map.entry("异常","exception"), Map.entry("继承","inheritance"), Map.entry("封装","encapsulation"),
        Map.entry("多态","polymorphism"), Map.entry("接口","interface"), Map.entry("抽象","abstract"),
        Map.entry("构造","constructor"), Map.entry("检索","retrieval"), Map.entry("提示词","prompt"),
        Map.entry("数组","array"), Map.entry("字符串","string"), Map.entry("循环","loop"));
    private final List<Chunk> chunks;
    private final Map<String,List<Posting>> postings = new HashMap<>();
    private final int[] lengths;
    private final double average;

    public Bm25Index(List<Chunk> source) {
        chunks = List.copyOf(source); lengths = new int[chunks.size()];
        for (int i=0;i<chunks.size();i++) {
            Map<String,Integer> freq = new HashMap<>();
            for (String t : tokens(chunks.get(i).text())) freq.merge(t,1,Integer::sum);
            lengths[i] = freq.values().stream().mapToInt(Integer::intValue).sum();
            for (var e:freq.entrySet()) postings.computeIfAbsent(e.getKey(), x->new ArrayList<>()).add(new Posting(i,e.getValue()));
        }
        average = Math.max(1, Arrays.stream(lengths).average().orElse(1));
    }

    public List<Hit> search(String query, int limit) {
        Set<String> terms = new HashSet<>(tokens(query));
        if (terms.isEmpty() || chunks.isEmpty()) return List.of();
        Map<Integer,Double> scores = new HashMap<>();
        Map<Integer,Integer> matches = new HashMap<>();
        for (String term:terms) {
            var list = postings.getOrDefault(term,List.of());
            double idf = Math.log(1 + (chunks.size() - list.size() + 0.5)/(list.size()+0.5));
            for (Posting p:list) {
                double s = idf * p.count * 2.2 / (p.count + 1.2*(0.25 + 0.75*lengths[p.id]/average));
                scores.merge(p.id,s,Double::sum); matches.merge(p.id,1,Integer::sum);
            }
        }
        return scores.entrySet().stream()
            .filter(e->e.getValue()>=1.0 && matches.get(e.getKey())/(double)terms.size()>=0.20)
            .sorted(Map.Entry.<Integer,Double>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
            .limit(Math.max(0,limit)).map(e->new Hit(chunks.get(e.getKey()),e.getValue())).toList();
    }

    public List<Chunk> chunks() { return chunks; }

    static List<String> tokens(String input) {
        String text = input.replaceAll("([a-z])([A-Z])", "$1 $2").toLowerCase(Locale.ROOT);
        // Short Chinese topic aliases help queries against the English course slides.
        for (var e:ALIASES.entrySet()) if(text.contains(e.getKey())) text += " " + e.getValue();
        var matcher=TOKEN.matcher(text);
        List<String> result=new ArrayList<>();
        while(matcher.find()) {
            String t=matcher.group();
            if(t.length()>4 && t.endsWith("s") && !t.endsWith("ss")) t=t.substring(0,t.length()-1);
            if(t.length()>1 && !STOP.contains(t)) result.add(t);
        }
        return result;
    }
}
