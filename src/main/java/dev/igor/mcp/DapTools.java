package dev.igor.mcp;

import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DapTools {
    @Inject
    DapClient dap;

    @Tool(name = "dapAttach", description = "Attach to a Debug Adapter server")
    public String attach(
            @ToolArg(description = "Host") String host,
            @ToolArg(description = "Port") int port) throws Exception {
        dap.connect(host, port);
        var init = dap.initialize();
        dap.configurationDone();
        return Json.write(init);
    }

    @Tool(name = "dapThreads", description = "List threads")
    public String threads() throws Exception {
        return Json.write(dap.threads());
    }

    @Tool(name = "dapStackTrace", description = "Get stack trace")
    public String stackTrace(
            @ToolArg(description = "Thread id") int threadId,
            @ToolArg(description = "Start frame (optional)") Integer startFrame,
            @ToolArg(description = "Levels (optional)") Integer levels) throws Exception {
        return Json.write(dap.stackTrace(threadId, startFrame, levels));
    }

    @Tool(name = "dapScopes", description = "Get scopes for a frame")
    public String scopes(@ToolArg(description = "Frame id") int frameId) throws Exception {
        return Json.write(dap.scopes(frameId));
    }

    @Tool(name = "dapVariables", description = "Get variables for a scope")
    public String variables(@ToolArg(description = "Variables reference") int variablesReference) throws Exception {
        return Json.write(dap.variables(variablesReference));
    }

    @Tool(name = "dapEvaluate", description = "Evaluate an expression")
    public String evaluate(
            @ToolArg(description = "Expression to evaluate") String expression,
            @ToolArg(description = "Frame id (optional)") Integer frameId,
            @ToolArg(description = "Context (optional)") String context) throws Exception {
        return Json.write(dap.evaluate(expression, frameId, context));
    }

    @Tool(name = "dapContinue", description = "Continue execution")
    public String cont(@ToolArg(description = "Thread id") int threadId) throws Exception {
        return Json.write(dap.cont(threadId));
    }
}
