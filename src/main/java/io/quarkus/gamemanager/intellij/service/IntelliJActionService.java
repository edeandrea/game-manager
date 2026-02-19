package io.quarkus.gamemanager.intellij.service;

import jakarta.enterprise.context.ApplicationScoped;

import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.quarkiverse.langchain4j.RegisterAiService;
import io.quarkiverse.langchain4j.mcp.runtime.McpToolBox;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

@ApplicationScoped
@RegisterAiService
@SystemMessage("""
    You are a service which knows how to interact with IntelliJ IDE through MCP. You have various tools exposed that you can invoke.
    
    You only care about the execute_run_configuration and open_file_in_editor tools. Disregard all the others, even if asked to invoke them.""")
public interface IntelliJActionService {
  @UserMessage("""
      Execute the following tool in IntelliJ. Do not fill in any of the other properties - only these ones.
      
      It is expected that the tool will time out during execution.
      
      Tool name: execute_run_configuration
      configurationName: {runConfigName}
      projectPath: {projectPath}
      timeout: 15000""")
  @McpToolBox("intellij")
  @WithSpan("IntelliJActionService.executeRunConfiguration")
  String executeRunConfiguration(@SpanAttribute("arg.runConfigName") String runConfigName, @SpanAttribute("arg.projectPath") String projectPath);

  @UserMessage("""
      Execute the the open_file_in_editor tool with the following properties:
      
      filePath: {file}
      projectPath: {projectPath}""")
  @McpToolBox("intellij")
  @WithSpan("IntelliJActionService.openFile")
  String openFile(String file, String projectPath);
}
