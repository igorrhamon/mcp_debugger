package dev.igor.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class Json {
    private static final ObjectMapper M = new ObjectMapper();
    public static Map<String,Object> readObj(String s) throws RuntimeException {
        try { return M.readValue(s, Map.class); } catch (Exception e) { throw new RuntimeException(e); }
    }
    public static byte[] writeBytes(Object o) throws RuntimeException {
        try { return M.writeValueAsBytes(o); } catch (Exception e) { throw new RuntimeException(e); }
    }
    public static String write(Object o) { return new String(writeBytes(o), StandardCharsets.UTF_8); }
}
