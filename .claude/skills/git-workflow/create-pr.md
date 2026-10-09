# Create a PR

1. **First, follow [push.md](push.md)** (which itself starts with [commit.md](commit.md)) so all changes are committed and pushed. If the user started on `main`, that flow will have checked out a new feature branch; the PR is opened from that new branch (head) into `main` (base), never from `main` itself.
2. Check whether a PR already exists for this branch: `gh pr view --json url,state 2>/dev/null`. If an open one exists, report its URL and stop. Pushing already updated it.
3. Gather context: `git log main..HEAD --oneline` and `git diff main...HEAD --stat`. Base the PR on **all** commits in the branch, not just the last one.
4. Create the PR against `main`:
   - **Title**: short imperative subject (≤72 chars). Use the commit subject if there is one commit; otherwise summarize.
   - **Body** (via HEREDOC):

     ```
     ## Summary
     - <1–3 bullets on what changed and why>

     ## Test plan
     - [ ] ./gradlew ktlintCheck
     - [ ] ./gradlew test
     - [ ] <manual check relevant to the change>

     🤖 Generated with [Claude Code](https://claude.com/claude-code)
     ```

   - Command: `gh pr create --base main --title "<title>" --body "$(cat <<'EOF' ... EOF)"`.
   - Only tick test-plan items that were actually run.
5. Return the PR URL to the user.
6. In the desktop app: call the `ccd_pr` `get_status` tool and, if it doesn't report this PR, `bind_pr` it. Offer Auto-fix based on CI. Don't poll CI yourself and don't enable auto-merge unless asked.
