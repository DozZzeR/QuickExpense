# Example: Creating a "Log Cleaner" Skill

1. Create directory: `.agent/skills/log_cleaner/`
2. Create `SKILL.md`:
```yaml
---
name: log_cleaner
description: Removes debug logs from Kotlin files before release.
---
# Log Cleaner
Mission: Ensure no sensitive info is leaked via logs in production.
...
```
3. Create script: `scripts/clean_logs.py`
4. Register the skill in `.agent/skills/` (it's automatically discovered).
