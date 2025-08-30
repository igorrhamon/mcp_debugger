package dev.igor.mcp;

import dev.igor.mcp.tools.Tools;
import io.quarkus.picocli.runtime.annotations.TopCommand;
import picocli.CommandLine.Command;

import java.util.List;

@TopCommand
@Command(name = "quarkus-mcp-dap", mixinStandardHelpOptions = true)
public class QuarkusMcpCommand implements Runnable {
    public void run() {
        DapClient dap = new DapClient();
        McpServer mcp = new McpServer(
                List.of(
                        Tools.attach(dap),
                        Tools.threads(dap),
                        Tools.stackTrace(dap),
                        Tools.scopes(dap),
                        Tools.variables(dap),
                        Tools.evaluate(dap),
                        Tools.cont(dap)
                ),
                System.in,
                System.out
        );
        mcp.run();
        try { Thread.currentThread().join(); } catch (InterruptedException ignored) {}
    }
}
