package com.example.mcp.prompts;

import java.util.List;
import java.util.Map;

/**
 * SDLC workflow prompt that guides through the complete software development lifecycle.
 *
 * <p>This prompt provides a structured workflow following the SDLC personas:
 * <ol>
 *   <li>Analyst - Requirements analysis and impact assessment</li>
 *   <li>Architect - Solution design and technical decisions</li>
 *   <li>Developer - Implementation following best practices</li>
 *   <li>Tester - Comprehensive testing strategy</li>
 *   <li>Reviewer - Code quality review</li>
 *   <li>Documentor - Technical documentation</li>
 * </ol>
 *
 * @author Maven SDLC Team
 * @version 2.0.0
 */
public class SdlcWorkflowPrompt implements Prompt {

    @Override
    public String getName() {
        return "sdlc-full-workflow";
    }

    @Override
    public String getDescription() {
        return "Complete SDLC workflow from analysis through documentation for implementing a feature or fixing a bug";
    }

    @Override
    public List<Map<String, Object>> getArguments() {
        return List.of(
                Map.of(
                        "name", "projectPath",
                        "description", "Path to the Maven project",
                        "required", true
                ),
                Map.of(
                        "name", "task",
                        "description", "Feature to implement or bug to fix",
                        "required", true
                ),
                Map.of(
                        "name", "type",
                        "description", "Task type: 'feature' or 'bugfix'",
                        "required", false
                )
        );
    }

    @Override
    public String getPrompt(Map<String, Object> arguments) throws Exception {
        String projectPath = (String) arguments.get("projectPath");
        String task = (String) arguments.get("task");
        String type = (String) arguments.getOrDefault("type", "feature");

        if (projectPath == null || task == null) {
            throw new IllegalArgumentException("projectPath and task are required");
        }

        return String.format("""
                # SDLC Workflow: %s

                Project: %s
                Task: %s

                ## Phase 1: Analysis (Analyst Persona)

                **Objective:** Understand the codebase and assess impact

                **Steps:**
                1. Run `analyze-maven-project` on %s
                2. Run `analyze-dependencies` to understand dependency structure
                3. Run `code-quality-check` to assess current code quality
                4. Analyze the codebase structure relevant to: "%s"

                **Deliverable:** Analysis report covering:
                - Current state of relevant modules
                - Dependencies and potential conflicts
                - Impact assessment (files affected, complexity)
                - Risks and considerations

                ---

                ## Phase 2: Architecture (Architect Persona)

                **Objective:** Design the solution

                **Steps:**
                1. Review the analysis report from Phase 1
                2. Run `suggest-implementation` for architectural guidance
                3. Design the solution following existing patterns
                4. Identify which modules/packages will be modified

                **Deliverable:** Architecture document covering:
                - Solution design and approach
                - Module/class structure changes
                - Integration points
                - Trade-offs and alternatives considered

                ---

                ## Phase 3: Implementation (Developer Persona)

                **Objective:** Implement the solution with clean code

                **Steps:**
                1. Run `implement-feature` (for features) or `fix-bug` (for bugs)
                2. Follow the architecture design from Phase 2
                3. Write clean, maintainable code following project conventions
                4. Add proper error handling and logging

                **Deliverable:** Implementation including:
                - Source code changes
                - Proper JavaDoc documentation
                - Inline comments for complex logic
                - Error handling

                ---

                ## Phase 4: Testing (Tester Persona)

                **Objective:** Ensure comprehensive test coverage

                **Steps:**
                1. Run `generate-tests` for new/modified classes
                2. Write integration tests
                3. Test edge cases and error conditions
                4. Run `code-quality-check` to verify quality

                **Deliverable:** Test suite including:
                - Unit tests (target: 80%%+ coverage)
                - Integration tests
                - Edge case tests
                - Test documentation

                ---

                ## Phase 5: Review (Reviewer Persona)

                **Objective:** Ensure code quality and best practices

                **Steps:**
                1. Run `code-quality-check` for static analysis
                2. Run `security-scan` for vulnerability detection
                3. Review code for:
                   - Adherence to SOLID principles
                   - Proper error handling
                   - Performance considerations
                   - Security best practices

                **Deliverable:** Review report covering:
                - Code quality assessment
                - Security findings
                - Performance concerns
                - Recommendations for improvement

                ---

                ## Phase 6: Documentation (Documentor Persona)

                **Objective:** Create comprehensive documentation

                **Steps:**
                1. Run `generate-documentation` for JavaDoc
                2. Update README if needed
                3. Create/update API documentation
                4. Document any breaking changes

                **Deliverable:** Documentation including:
                - Updated JavaDoc
                - README updates
                - API documentation
                - Changelog entry

                ---

                ## Final Checklist

                Before considering the task complete, verify:

                - [ ] All phases completed
                - [ ] Code quality check passes
                - [ ] Security scan shows no critical issues
                - [ ] Tests pass with 80%%+ coverage
                - [ ] Documentation is complete and accurate
                - [ ] Code follows project conventions
                - [ ] No breaking changes (or documented if unavoidable)

                **Estimated Timeline:**
                - Analysis: 30-60 minutes
                - Architecture: 30-60 minutes
                - Implementation: 2-4 hours
                - Testing: 1-2 hours
                - Review: 30 minutes
                - Documentation: 30 minutes

                **Total: 5-9 hours** for a complete, production-ready implementation
                """,
                type.equals("bugfix") ? "Bug Fix" : "Feature Implementation",
                projectPath,
                task,
                projectPath,
                task
        );
    }
}
