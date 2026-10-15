package dev.dualpower;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/** Pure-Java per-slot snapshot format; never modifies Awakened state. */
public final class IndependentSkillSnapshots {
    private final Map<String, Integer> first;
    private final Map<String, Integer> second;
    public IndependentSkillSnapshots(Map<String,Integer> first, Map<String,Integer> second) {
        this.first = validate(first); this.second = validate(second);
    }
    private static Map<String,Integer> validate(Map<String,Integer> input) {
        Objects.requireNonNull(input);
        Map<String,Integer> out = new HashMap<>();
        for (var entry : input.entrySet()) {
            String k = Objects.requireNonNull(entry.getKey());
            Integer v = Objects.requireNonNull(entry.getValue());
            if (!k.matches("[a-zA-Z0-9_.:-]{1,128}") || v < 0 || v > 1_000_000)
                throw new IllegalArgumentException("Invalid skill snapshot entry");
            out.put(k,v);
        }
        return Collections.unmodifiableMap(out);
    }
    public Map<String,Integer> first() { return first; }
    public Map<String,Integer> second() { return second; }
    public Properties encode() {
        Properties p = new Properties(); p.setProperty("schema", "1");
        first.forEach((k,v)->p.setProperty("first."+k, v.toString()));
        second.forEach((k,v)->p.setProperty("second."+k, v.toString()));
        return p;
    }
    public static IndependentSkillSnapshots decode(Properties p) {
        if (!"1".equals(p.getProperty("schema"))) throw new IllegalArgumentException("Unsupported schema");
        Map<String,Integer> a = new HashMap<>(), b = new HashMap<>();
        for (String k : p.stringPropertyNames()) {
            if (k.equals("schema")) continue;
            Map<String,Integer> target;
            String id;
            if (k.startsWith("first.")) { target=a; id=k.substring(6); }
            else if (k.startsWith("second.")) { target=b; id=k.substring(7); }
            else throw new IllegalArgumentException("Unknown snapshot field");
            target.put(id, Integer.parseInt(p.getProperty(k)));
        }
        return new IndependentSkillSnapshots(a,b);
    }
}
