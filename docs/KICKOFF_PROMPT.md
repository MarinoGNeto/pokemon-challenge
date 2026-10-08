# Kickoff prompt for the implementation session

Paste this as the first message of a new AI session opened on this repository.

```text
We are implementing the Java/Spring Boot + React technical exercise in this repository.
Read, in this order, before doing anything: CLAUDE.md, docs/REQUIREMENTS.md, docs/DECISIONS.md, docs/PLAN.md.

Step 1 — Pre-flight: check the local toolchain (java, maven/wrapper, node, npm, docker, git) and report
versions and anything missing. Do not install anything without asking.

Step 2 — Review docs/DECISIONS.md and point out anything you would challenge (versions, library
compatibility with the chosen Spring Boot version, scope risk vs. the Sunday 18:00 deadline).
Wait for my confirmation, then mark the decisions as Accepted.

Step 3 — Follow docs/PLAN.md section 1 (foundation), strictly TDD and small Conventional Commits:
backend scaffold, package skeleton, ArchUnit rule test first, docker-compose with Postgres, health check.

Rules for the whole session:
- Keep the REQUIREMENTS.md checkboxes and docs/AI_USAGE.md up to date as you go.
- Never add symlinks, secrets or the challenge PDF to git.
- Ask before any destructive or hard-to-reverse action (force push, deleting files, rewriting history).
```
