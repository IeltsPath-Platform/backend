---
date: 2026-09-16T08:30:00+07:00
topic: graphify-update-strategy
---

# Research Report: Updating the Graphify code graph

## Summary

Use an incremental `update` after a batch of code changes. It uses the existing
graph/cache, reparses changed code, removes nodes from deleted files, and
regenerates the graph artifacts locally. This is the recommended default for
this repository.

Use `watch` only during an active development session when immediate refresh is
valuable. It stays in the foreground and debounces changes, preventing one
rebuild per save.

Verified on this repository on 2026-09-16: `update --force` completed locally
and regenerated the graph artifacts (577 nodes, 1,666 edges).

## Commands

```powershell
$root = 'C:\Users\pduy8\OneDrive\Desktop\microservices-starter-template'
$env:PYTHONPATH = "$root\.graphify-tools"

# Run after a batch of Java, SQL, or config-code changes.
python -m graphify update $root

# Optional: continuously rebuild while this terminal remains open.
python -m graphify watch $root
```

`update` refreshes `graphify-out/graph.json`, `graphify-out/GRAPH_REPORT.md`,
and the interactive `graphify-out/graph.html`. `watch` applies the same local
code extraction after filesystem activity settles (default debounce: 3 seconds).

## Recommendation

1. Keep the existing code-only graph as the team default.
2. Run `update` before a handoff, architecture investigation, or pull request.
3. Start `watch` only for longer refactors. Stop it with `Ctrl+C` when done.
4. After a large deletion/refactor, use `python -m graphify update $root --force`
   if Graphify's node-count safety guard prevents replacement.

Do not enable a post-commit hook by default: it modifies generated graph
artifacts after commits, which adds worktree churn without helping an ordinary
editing session. Consider it only if committed graph artifacts are a team
requirement.

## Boundaries

The current graph is intentionally source-code-only: `.graphifyignore` excludes
Markdown as well as Graphify's runtime and output folders. Source-code changes
therefore update locally without an LLM or API. If the scope later includes
Markdown, PDFs, images, or other documents, adjust that ignore rule and use a
separate semantic extraction configuration, which may need an LLM-backed
update.

## Sources

- Local Graphify 0.9.62 CLI help, inspected 2026-09-16.
- [Graphify watcher behavior](https://github.com/Graphify-Labs/graphify/blob/v8/graphify/skills/agents/references/add-watch.md)
- [Graphify watcher implementation](https://github.com/Graphify-Labs/graphify/blob/v8/graphify/watch.py)
- [Graphify release notes: deleted-node reconciliation and hook behavior](https://github.com/safishamsi/graphify/releases)

## Unresolved questions

None. The recommended workflow does not require a new service, API key, or
repository hook.
