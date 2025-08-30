package dev.igor.mcp;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class McpServer {
    public interface Tool {
        String name();
        Map<String,Object> schema();
        Map<String,Object> call(Map<String,Object> args) throws Exception;
    }

    private final List<Tool> tools;
    private final InputStream in;
    private final OutputStream out;

    public McpServer(List<Tool> tools, InputStream in, OutputStream out) {
        this.tools = tools;
        this.in = in;
        this.out = out;
    }

    public void run() {
        new Thread(this::loop, "mcp-loop").start();
    }

    private void loop() {
        try {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            byte[] tmp = new byte[8192];
            while (true) {
                int r = in.read(tmp);
                if (r < 0) break;
                buf.write(tmp, 0, r);
                byte[] all = buf.toByteArray();
                int idx;
                while ((idx = indexOf(all, "\r\n\r\n".getBytes(StandardCharsets.UTF_8))) >= 0) {
                    String header = new String(Arrays.copyOfRange(all, 0, idx), StandardCharsets.UTF_8);
                    int contentLength = parseContentLength(header);
                    int start = idx + 4;
                    if (all.length - start < contentLength) break;
                    String body = new String(Arrays.copyOfRange(all, start, start + contentLength), StandardCharsets.UTF_8);
                    Map<String,Object> req = Json.readObj(body);
                    Map<String,Object> resp = handle(req);
                    write(resp);
                    all = Arrays.copyOfRange(all, start + contentLength, all.length);
                }
                buf.reset();
                buf.write(all);
            }
        } catch (Exception ignored) {}
    }

    private Map<String,Object> handle(Map<String,Object> req) {
        String idKey = "id";
        Object id = req.get(idKey);
        String method = String.valueOf(req.get("method"));
        Map<String,Object> result;
        try {
            if ("tools/list".equals(method)) {
                List<Map<String,Object>> t = new ArrayList<>();
                for (Tool tool : tools) {
                    Map<String,Object> e = new HashMap<>();
                    e.put("name", tool.name());
                    e.put("description", tool.name());
                    e.put("inputSchema", tool.schema());
                    t.add(e);
                }
                result = Map.of("tools", t);
                return respOk(id, result);
            } else if ("tools/call".equals(method)) {
                Map<String,Object> params = (Map<String,Object>) req.get("params");
                String name = String.valueOf(params.get("name"));
                Map<String,Object> args = (Map<String,Object>) params.getOrDefault("arguments", Collections.emptyMap());
                Optional<Tool> t = tools.stream().filter(x -> x.name().equals(name)).findFirst();
                if (t.isEmpty()) return respErr(id, -32601, "Tool not found");
                Map<String,Object> out = t.get().call(args);
                return respOk(id, Map.of("content", List.of(Map.of("type","text","text", Json.write(out)))));
            } else if ("ping".equals(method)) {
                return respOk(id, Map.of("ok", true));
            } else {
                return respErr(id, -32601, "Method not found");
            }
        } catch (Exception e) {
            return respErr(id, -32000, e.getMessage());
        }
    }

    private Map<String,Object> respOk(Object id, Object result) {
        Map<String,Object> m = new HashMap<>();
        m.put("jsonrpc","2.0");
        m.put("id", id);
        m.put("result", result);
        return m;
    }

    private Map<String,Object> respErr(Object id, int code, String msg) {
        Map<String,Object> m = new HashMap<>();
        m.put("jsonrpc","2.0");
        m.put("id", id);
        m.put("error", Map.of("code", code, "message", msg));
        return m;
    }

    private synchronized void write(Map<String,Object> msg) throws Exception {
        byte[] body = Json.writeBytes(msg);
        String h = "Content-Length: " + body.length + "\r\n\r\n";
        out.write(h.getBytes(StandardCharsets.UTF_8));
        out.write(body);
        out.flush();
    }

    private int indexOf(byte[] a, byte[] b) {
        outer: for (int i=0;i<=a.length-b.length;i++) {
            for (int j=0;j<b.length;j++) if (a[i+j]!=b[j]) continue outer;
            return i;
        }
        return -1;
    }

    private int parseContentLength(String header) {
        for (String line : header.split("\r\n")) {
            int i = line.toLowerCase().indexOf("content-length:");
            if (i==0) return Integer.parseInt(line.substring(15).trim());
        }
        return 0;
    }
}
