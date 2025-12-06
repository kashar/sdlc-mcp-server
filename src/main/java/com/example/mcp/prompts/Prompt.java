package com.example.mcp.prompts;

import java.util.List;
import java.util.Map;

/**
 * Interface for MCP prompts.
 *
 * <p>Prompts are pre-defined workflows that guide users through complex operations
 * by providing structured, multi-step instructions.
 *
 * <h2>Use Cases:</h2>
 * <ul>
 *   <li>SDLC workflows (Analyst → Architect → Developer → Tester)</li>
 *   <li>Onboarding guides for new projects</li>
 *   <li>Best practice checklists</li>
 *   <li>Troubleshooting wizards</li>
 * </ul>
 *
 * @author Maven SDLC Team
 * @version 2.0.0
 */
public interface Prompt {

    /**
     * Gets the prompt name.
     *
     * @return the prompt name
     */
    String getName();

    /**
     * Gets the prompt description.
     *
     * @return the prompt description
     */
    String getDescription();

    /**
     * Gets the list of arguments this prompt accepts.
     *
     * @return list of argument definitions
     */
    List<Map<String, Object>> getArguments();

    /**
     * Gets the prompt content/template.
     *
     * @param arguments the arguments provided by the user
     * @return the formatted prompt content
     * @throws Exception if prompt generation fails
     */
    String getPrompt(Map<String, Object> arguments) throws Exception;
}
