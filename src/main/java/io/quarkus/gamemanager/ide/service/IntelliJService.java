package io.quarkus.gamemanager.ide.service;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkiverse.langchain4j.RegisterAiService;
import io.quarkiverse.langchain4j.mcp.runtime.McpToolBox;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

@ApplicationScoped
@RegisterAiService
@SystemMessage("""
    You are a service which knows how to interact with IntelliJ IDE through MCP. You have various tools exposed that you can invoke.
    
    You only care about the execute_run_configuration tool. Disregard all the others, even if asked to invoke them.
    """)
public interface IntelliJService {
  record Command(
      String command,

      boolean executeInShell,

      boolean reuseExistingTerminalWindow,

      String projectPath
  ) {}

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
  String executeTerminalCommand(Command command);

  @UserMessage("""
      Execute the following tool in IntelliJ. Do not fill in any of the other properties - only these ones.
      
      It is expected that the tool will time out during execution
      
      Tool name: execute_run_configuration
      configurationName: {runConfigName}
      projectPath: {projectPath}
      timeout: 15000
      """)
  @McpToolBox("intellij")
  void executeRunConfiguration(String runConfigName, String projectPath);
}
