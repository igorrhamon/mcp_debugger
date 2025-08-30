package dev.igor.mcp.tools;

import dev.igor.mcp.DapClient;
import dev.igor.mcp.McpServer;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Tools {
    public static McpServer.Tool attach(DapClient dap) {
        return new McpServer.Tool() {
            public String name() { return "dap.attach"; }
            public Map<String, Object> schema() {
                Map<String,Object> p = new HashMap<>();
                p.put("type","object");
                Map<String,Object> props = new HashMap<>();
                props.put("host", Map.of("type","string"));
                props.put("port", Map.of("type","number"));
                p.put("properties", props);
                p.put("required", new String[]{"host","port"});
                return p;
            }
            public Map<String, Object> call(Map<String, Object> args) throws Exception {
                String host = String.valueOf(args.get("host"));
                Number port = (Number) args.get("port");
                dap.connect(host, port.intValue());
                Map<String,Object> init = dap.initialize();
                dap.configurationDone();
                Map<String,Object> out = new HashMap<>();
                out.put("initialize", init);
                return out;
            }
        };
    }

    public static McpServer.Tool threads(DapClient dap) {
        return new McpServer.Tool() {
            public String name() { return "dap.threads"; }
            public Map<String, Object> schema() { return Map.of("type","object","properties", Collections.emptyMap()); }
            public Map<String, Object> call(Map<String, Object> args) throws Exception { return dap.threads(); }
        };
    }

    public static McpServer.Tool stackTrace(DapClient dap) {
        return new McpServer.Tool() {
            public String name() { return "dap.stackTrace"; }
            public Map<String, Object> schema() {
                return Map.of("type","object","properties", Map.of(
                        "threadId", Map.of("type","number"),
                        "startFrame", Map.of("type","number"),
                        "levels", Map.of("type","number")
                ), "required", new String[]{"threadId"});
            }
            public Map<String, Object> call(Map<String, Object> args) throws Exception {
                Number threadId = (Number) args.get("threadId");
                Number startFrame = (Number) args.get("startFrame");
                Number levels = (Number) args.get("levels");
                return dap.stackTrace(threadId, startFrame, levels);
            }
        };
    }

    public static McpServer.Tool scopes(DapClient dap) {
        return new McpServer.Tool() {
            public String name() { return "dap.scopes"; }
            public Map<String, Object> schema() {
                return Map.of("type","object","properties", Map.of(
                        "frameId", Map.of("type","number")
                ), "required", new String[]{"frameId"});
            }
            public Map<String, Object> call(Map<String, Object> args) throws Exception {
                Number frameId = (Number) args.get("frameId");
                return dap.scopes(frameId);
            }
        };
    }

    public static McpServer.Tool variables(DapClient dap) {
        return new McpServer.Tool() {
            public String name() { return "dap.variables"; }
            public Map<String, Object> schema() {
                return Map.of("type","object","properties", Map.of(
                        "variablesReference", Map.of("type","number")
                ), "required", new String[]{"variablesReference"});
            }
            public Map<String, Object> call(Map<String, Object> args) throws Exception {
                Number ref = (Number) args.get("variablesReference");
                return dap.variables(ref);
            }
        };
    }

    public static McpServer.Tool evaluate(DapClient dap) {
        return new McpServer.Tool() {
            public String name() { return "dap.evaluate"; }
            public Map<String, Object> schema() {
                return Map.of("type","object","properties", Map.of(
                        "expression", Map.of("type","string"),
                        "frameId", Map.of("type","number"),
                        "context", Map.of("type","string")
                ), "required", new String[]{"expression"});
            }
            public Map<String, Object> call(Map<String, Object> args) throws Exception {
                String exp = String.valueOf(args.get("expression"));
                Number frameId = (Number) args.get("frameId");
                String context = args.get("context")==null?null:String.valueOf(args.get("context"));
                return dap.evaluate(exp, frameId, context);
            }
        };
    }

    public static McpServer.Tool cont(DapClient dap) {
        return new McpServer.Tool() {
            public String name() { return "dap.continue"; }
            public Map<String, Object> schema() {
                return Map.of("type","object","properties", Map.of(
                        "threadId", Map.of("type","number")
                ), "required", new String[]{"threadId"});
            }
            public Map<String, Object> call(Map<String, Object> args) throws Exception {
                Number t = (Number) args.get("threadId");
                return dap.cont(t);
            }
        };
    }
}
