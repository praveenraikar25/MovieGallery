# Push

1. **First, follow [commit.md](commit.md)** to commit any outstanding changes. If the working tree is already clean and there are unpushed commits, skip straight to pushing.
2. If the user started on `main`, commit.md has already checked out a new feature branch and committed there. Push that new branch. Confirm the current branch is not `main` before pushing; never push to `main` directly. If you're on `main` with only unpushed commits and nothing to commit, create a new branch from it (`git switch -c feature/<slug>`) and push that.
3. Push: `git push -u origin <current-branch>` (the `-u` sets upstream on first push; plain `git push` is fine if upstream already exists).
4. Never use `--force` / `--force-with-lease`. If the push is rejected as non-fast-forward, stop, explain, and ask the user how to proceed (rebase vs. merge).
5. Report the branch, remote, and the commits that were pushed.
