---
name: sync-main
description: Switch to the main branch and update it from the remote. Use when the user says "switch to main", "checkout main", "go back to main", "update main", "pull main", or "sync main".
---

# Sync main

Switch to `main` and fast-forward it to `origin/main`. Run the steps in order.

## 1. Check for uncommitted changes

Run `git status --porcelain`.

If there is **any** output (staged, unstaged, or untracked files), do **not** switch branches and do **not** run any further git commands. Show the user the changed files and the current branch, then ask (AskUserQuestion) which action to take:

- **Commit** — commit them on the current branch first (follow the `git-workflow` skill), then continue.
- **Stash** — `git stash push -u -m "<branch>: before sync-main"`, then continue. Remind the user to `git stash pop` when they return to that branch.
- **Discard** — only after a second, explicit confirmation, since it is irreversible.
- **Cancel** — stay on the current branch and change nothing.

Never commit, stash, or discard without the user's choice. Continue to step 2 only if they chose commit, stash, or discard.

## 2. Fetch

```bash
git fetch origin --prune
```

## 3. Switch to main

```bash
git checkout main
```

Skip if already on `main`.

## 4. Update from remote

```bash
git pull --ff-only origin main
```

If this fails (local `main` has diverged from `origin/main`), report it and stop. Never merge, rebase, `reset --hard`, or force.

## 5. Report

State the current branch, the latest commit (`git log -1 --oneline`), and whether `main` was already up to date or was fast-forwarded.

## Rules

- Only switch and update. Never push, and never delete the previous branch.
- Never use `--no-verify` or force flags.
