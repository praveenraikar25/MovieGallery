# Commit

1. Inspect the state in parallel: `git status`, `git diff` and `git diff --staged`, `git log -5 --oneline`, `git branch --show-current`.
2. If there is nothing to commit, say so and stop.
3. **Branch check (do this before staging or committing).** If `git branch --show-current` is `main`, check out a new branch first: `git switch -c feature/<short-kebab-slug>` (slug from the change). Uncommitted changes carry over to the new branch. Then commit there. This applies whether the request was commit, push, or create a PR. If already on another branch, stay on it.
4. Stage files **by name**, not `git add -A` / `git add .`.
   - Never stage `local.properties`, `app/google-services.json`, `.env`, keystores, or anything that looks like a secret.
   - If there are untracked or unrelated files (scratch HTML, exports, etc.), list them and ask the user whether to include them. Don't guess.
5. Write the message in the style of the repo history:
   - Imperative subject, no trailing period, ≤72 characters (e.g. "Fix four issues found reviewing the poster-search diff").
   - Optional body after a blank line explaining *why*, wrapped at ~72 columns.
   - End with the `Co-Authored-By` trailer from the session's attribution reminder.
   - Pass the message via a HEREDOC.
6. Commit. The `pre-commit` hook runs ktlint format and re-stages fixes.
   - If the hook blocks on something ktlint can't fix, fix it, re-stage, and make a **new** commit. Don't use `--no-verify` or `SKIP_KTLINT`.
7. Run `git status` to confirm and report the commit hash, subject, and files included.
