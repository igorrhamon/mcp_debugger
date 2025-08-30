package dev.igor.mcp;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DapClient {
    private Socket socket;
    private InputStream in;
    private OutputStream out;
    private int seq = 1;
    private final Map<Integer, java.util.function.Consumer<Map<String,Object>>> pending = new ConcurrentHashMap<>();
    private volatile boolean running;

    public void connect(String host, int port) throws Exception {
        socket = new Socket(host, port);
        socket.setTcpNoDelay(true);
        in = socket.getInputStream();
        out = socket.getOutputStream();
        running = true;
        new Thread(this::readerLoop, "dap-reader").start();
    }

    private void readerLoop() {
        try {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            byte[] tmp = new byte[8192];
            while (running) {
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
                    Map<String,Object> msg = Json.readObj(body);
                    if ("response".equals(msg.get("type"))) {
                        Number rs = (Number) msg.get("request_seq");
                        if (rs != null) {
                            java.util.function.Consumer<Map<String,Object>> c = pending.remove(rs.intValue());
                            if (c != null) c.accept(msg);
                        }
                    }
                    all = Arrays.copyOfRange(all, start + contentLength, all.length);
                }
                buf.reset();
                buf.write(all);
            }
        } catch (Exception ignored) {}
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

    private synchronized void send(Map<String,Object> m) throws Exception {
        byte[] body = Json.writeBytes(m);
        String h = "Content-Length: " + body.length + "\r\n\r\n";
        out.write(h.getBytes(StandardCharsets.UTF_8));
        out.write(body);
        out.flush();
    }

    private Map<String,Object> request(String cmd, Map<String,Object> args) throws Exception {
        int id = seq++;
        Map<String,Object> req = new HashMap<>();
        req.put("seq", id);
        req.put("type","request");
        req.put("command", cmd);
        if (args!=null) req.put("arguments", args);
        final Map<String,Object>[] resHolder = new Map[1];
        Object lock = new Object();
        pending.put(id, r -> { synchronized (lock) { resHolder[0]=r; lock.notifyAll(); }});
        send(req);
        synchronized (lock) { while (resHolder[0]==null) lock.wait(); }
        return resHolder[0];
    }

    public Map<String,Object> initialize() throws Exception {
        Map<String,Object> a = new HashMap<>();
        a.put("clientID","mcp");
        a.put("clientName","mcp");
        a.put("adapterID","custom");
        a.put("linesStartAt1", true);
        a.put("columnsStartAt1", true);
        a.put("pathFormat","path");
        return request("initialize", a);
    }

    public Map<String,Object> configurationDone() throws Exception {
        return request("configurationDone", Collections.emptyMap());
    }

    public Map<String,Object> threads() throws Exception {
        return request("threads", Collections.emptyMap());
    }

    public Map<String,Object> stackTrace(Number threadId, Number startFrame, Number levels) throws Exception {
        Map<String,Object> a = new HashMap<>();
        a.put("threadId", threadId);
        if (startFrame!=null) a.put("startFrame", startFrame);
        if (levels!=null) a.put("levels", levels);
        return request("stackTrace", a);
    }

    public Map<String,Object> scopes(Number frameId) throws Exception {
        Map<String,Object> a = new HashMap<>();
        a.put("frameId", frameId);
        return request("scopes", a);
    }

    public Map<String,Object> variables(Number variablesReference) throws Exception {
        Map<String,Object> a = new HashMap<>();
        a.put("variablesReference", variablesReference);
        return request("variables", a);
    }

    public Map<String,Object> evaluate(String expression, Number frameId, String context) throws Exception {
        Map<String,Object> a = new HashMap<>();
        a.put("expression", expression);
        if (frameId!=null) a.put("frameId", frameId);
        if (context!=null) a.put("context", context);
        return request("evaluate", a);
    }

    public Map<String,Object> cont(Number threadId) throws Exception {
        Map<String,Object> a = new HashMap<>();
        a.put("threadId", threadId);
        return request("continue", a);
    }
}
