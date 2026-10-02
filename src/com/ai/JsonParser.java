package com.ai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonParser {

    private final String src;
    private int pos;

    public JsonParser(String src) {
        this.src = src;
        this.pos = 0;
    }

    public Object parse() {
        skipWhitespace();
        char c = src.charAt(pos);
        if (c == '{') return parseObject();
        if (c == '[') return parseArray();
        if (c == '"') return parseString();
        if (c == 't' || c == 'f') return parseBoolean();
        if (c == 'n') { pos += 4; return null; }
        return parseNumber();
    }

    private Map<String, Object> parseObject() {
        Map<String, Object> obj = new HashMap<>();
        pos++;
        skipWhitespace();
        if (src.charAt(pos) == '}') { pos++; return obj; }
        while (true) {
            skipWhitespace();
            String key = parseString();
            skipWhitespace();
            pos++;
            skipWhitespace();
            Object val = parse();
            obj.put(key, val);
            skipWhitespace();
            char c = src.charAt(pos++);
            if (c == '}') break;
        }
        return obj;
    }

    private List<Object> parseArray() {
        List<Object> list = new ArrayList<>();
        pos++;
        skipWhitespace();
        if (src.charAt(pos) == ']') { pos++; return list; }
        while (true) {
            skipWhitespace();
            Object val = parse();
            list.add(val);
            skipWhitespace();
            char c = src.charAt(pos++);
            if (c == ']') break;
        }
        return list;
    }

    private String parseString() {
        pos++;
        StringBuilder sb = new StringBuilder();
        while (true) {
            char c = src.charAt(pos++);
            if (c == '"') break;
            if (c == '\\') {
                char next = src.charAt(pos++);
                switch (next) {
                    case 'n': sb.append('\n'); break;
                    case 't': sb.append('\t'); break;
                    case 'r': sb.append('\r'); break;
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    default: sb.append(next);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private Double parseNumber() {
        int start = pos;
        while (pos < src.length() && (Character.isDigit(src.charAt(pos))
                || src.charAt(pos) == '.' || src.charAt(pos) == '-'
                || src.charAt(pos) == 'e' || src.charAt(pos) == 'E'
                || src.charAt(pos) == '+')) {
            pos++;
        }
        return Double.parseDouble(src.substring(start, pos));
    }

    private Boolean parseBoolean() {
        if (src.charAt(pos) == 't') { pos += 4; return Boolean.TRUE; }
        pos += 5;
        return Boolean.FALSE;
    }

    private void skipWhitespace() {
        while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) {
            pos++;
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> asObject(Object o) {
        return (Map<String, Object>) o;
    }

    @SuppressWarnings("unchecked")
    public static List<Object> asArray(Object o) {
        return (List<Object>) o;
    }

    public static double asDouble(Object o) {
        return ((Number) o).doubleValue();
    }

    public static int asInt(Object o) {
        return ((Number) o).intValue();
    }

    public static String asString(Object o) {
        return (String) o;
    }
}