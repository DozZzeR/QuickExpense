---
name: skill_describer
description: A skill to help create and document other Antigravity skills following standard structure.
---

# Skill Describer

This skill provides a standard framework and instructions for creating new Antigravity skills. It ensures that all skills follow the required structure: `SKILL.md` with metadata, `scripts/`, `examples/`, and `resources/`.

## Mission
To maintain high quality and consistency of agent capabilities by providing clear guidelines and templates for skill creation.

## Usage Instructions

### 1. Structure of a Skill
Every skill must reside in `.agent/skills/<skill_name>/` and contain:
- **SKILL.md** (Required): YAML frontmatter with `name` and `description`, followed by markdown documentation.
- **scripts/** (Optional): Executable logic.
- **examples/** (Optional): Usage patterns.
- **resources/** (Optional): Static assets or templates.

### 2. Creating a new SKILL.md
The `SKILL.md` file should start with:
```yaml
---
name: your_skill_name
description: Brief summary of what the skill does (acts as a trigger for the AI).
---
```

Followed by sections:
- # Skill Name
- ## Mission
- ## Usage Instructions
- ## Best Practices

### 3. Best Practices
- **Atomic**: One skill should do one thing well.
- **Descriptive**: The `description` in YAML is critical for the agent to know when to use the skill.
- **Documented**: Provide examples in the `examples/` folder.
