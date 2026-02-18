package io.quarkus.gamemanager.intellij.service;

import jakarta.enterprise.context.ApplicationScoped;

import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.quarkiverse.langchain4j.RegisterAiService;
import io.quarkiverse.langchain4j.mcp.runtime.McpToolBox;

import dev.langchain4j.model.output.structured.Description;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

@ApplicationScoped
@RegisterAiService
@SystemMessage("""
    You are a service which knows how to interact with IntelliJ IDE through MCP. You have various tools exposed that you can invoke.
    
    You only care about the execute_run_configuration tool. Disregard all the others, even if asked to invoke them.
    """)
public interface IntelliJActionService {
  @Description("A terminal command to execute in IntelliJ")
  record TerminalCommand(
      @Description("The terminal command to execute in IntelliJ")
      String command,

      @Description("Whether to execute the command in the user's shell shell")
      boolean executeInShell,

      @Description("Whether to reuse an existing terminal window")
      boolean reuseExistingTerminalWindow,

      @Description("The path to the project to execute the command in")
      String projectPath
  ) {
    public TerminalCommand(String command, String projectPath) {
      this(command, true, false, projectPath);
    }
  }

  @UserMessage("""
      Execute the following command in an IntelliJ terminal:
      
      command: {command.command}
      executeInShell: {command.executeInShell}
      reuseExistingTerminalWindow: {command.reuseExistingTerminalWindow}
      projectPath: {command.projectPath}
      timeout: none
      maxLinesCount: none
      truncateMode: do not truncate at all
      """)
  @McpToolBox("intellij")
  String executeTerminalCommand(TerminalCommand command);

  @UserMessage("""
      Execute the following tool in IntelliJ. Do not fill in any of the other properties - only these ones.
      
      It is expected that the tool will time out during execution.
      
      Tool name: execute_run_configuration
      configurationName: {runConfigName}
      projectPath: {projectPath}
      timeout: 15000
      """)
  @McpToolBox("intellij")
  @WithSpan("IntelliJActionService.executeRunConfiguration")
  String executeRunConfiguration(@SpanAttribute("arg.runConfigName") String runConfigName, @SpanAttribute("arg.projectPath") String projectPath);
}
