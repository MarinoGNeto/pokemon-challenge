# AI usage log

The evaluation explicitly rewards *fluency with GenAI tools, prompt engineering and critical thinking about
AI-generated code*. This log records, as the work happens, how AI was used on the **main project**
(the separate GenAI exercise is in [`../genai-exercise/`](../genai-exercise/README.md)).

**Tooling:** Claude (Claude Code / Claude app) as pair programmer. Project context and guard-rails are given to the
model through [`CLAUDE.md`](../CLAUDE.md) (constraints, architecture rules, TDD, error contract).

**Principles followed**
- I own the design: decisions are recorded in `DECISIONS.md` *before* asking the AI to implement them.
- Tests are the acceptance criteria for AI output: red test first, AI may propose the implementation, I review the diff.
- Nothing about PokeAPI is trusted from model memory: payloads are verified against real responses / fixtures.
- Every AI-generated diff is read line by line; I must be able to explain it in the code review.

## Entry template

```
### YYYY-MM-DD HH:MM — <short title>
- Goal:
- Prompt (summary or verbatim):
- Output accepted:
- Rejected / corrected (and why):
- How validated (tests, docs, manual check):
```

## Log

### 2026-10-08 — Environment preparation
- Goal: turn the challenge PDF into a working plan before writing code.
- Prompt (summary): "Prepare the repository so the challenge can start in a new session and successfully achieve the objectives"
- Output accepted: requirement digest with checklist, delivery plan, ADR log (proposed), `CLAUDE.md` guard-rails,
  git hygiene files (`.gitignore`, `.gitattributes`, `.editorconfig`), README skeleton, GenAI exercise skeleton.
- Corrected / decided by me: architecture decisions left as *Proposed* until I confirm them; challenge PDF
  excluded from the public repo.
- How validated: cross-checked every bullet of the PDF against `REQUIREMENTS.md`.
