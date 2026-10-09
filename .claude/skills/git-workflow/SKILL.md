---
name: git-workflow
description: Commit, push, or open a pull request for the current changes. Use when the user says "commit", "commit this", "push", "commit and push", "create a PR", "open a PR", or "make a pull request".
---

# Git workflow: commit → push → PR

Three levels, each building on the one below it. Pick the file that matches what the user asked for and follow it exactly.

| User asks for | Follow |
|---|---|
| Commit | [commit.md](commit.md) |
| Push (commit + push) | [push.md](push.md) |
| Create a PR (commit + push + PR against `main`) | [create-pr.md](create-pr.md) |

## Chaining rule

`create-pr.md` starts by following `push.md`, and `push.md` starts by following `commit.md`. Never skip the lower steps, and never repeat one that has already been done in this run (e.g. if the changes are already committed, don't re-commit).

## Rules that apply to every level

- Only act on the level requested. "Commit" must not push; "push" must not open a PR.
- Never force-push, never `--no-verify`, never amend or rewrite existing commits unless the user explicitly asks.
- Never commit directly to `main`. If the current branch is `main`, check out a new feature branch before committing. This applies to commit, push, and create PR alike.
- Never stage or commit secrets/local config: `local.properties`, `app/google-services.json`, `.env`, keystores, tokens.
- Follow the attribution lines given in the session's system reminder for commit messages and PR bodies.
