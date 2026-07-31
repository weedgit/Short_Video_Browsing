# Contributing Guide

## Branch policy

| Rule | Description |
|---|---|
| Protected `master` | **No one** pushes to `master` directly — including admins |
| One branch per change | Each feature or fix uses its own branch |
| Small commits | One commit = one logical change; avoid large mixed commits |
| PR required | All changes enter `master` only through a reviewed Pull Request |
| Rebase on conflict | Do **not** merge `master` into your branch; **rebase** onto `master` |

## Before starting work

Always sync `master` first, then create a branch:

```bash
git checkout master
git pull origin master
git checkout -b feat/short-description   # or fix/short-description
```

## Branch naming

```text
feat/<kebab-case-description>
fix/<kebab-case-description>
```

Examples:

- `feat/phase-1-home-feed-ui`
- `feat/auth-login-screen`
- `fix/bottom-bar-selected-state`
- `fix/gradle-wrapper-missing`

## Commit messages

Use [Conventional Commits](https://www.conventionalcommits.org/) with these prefixes:

```text
feat: add vertical feed pager placeholder
fix: correct bottom navigation selected state
```

Rules:

- Start with `feat:` or `fix:` (lowercase, colon, space)
- Use imperative mood (`add`, not `added`)
- Keep the subject line under 72 characters
- One logical change per commit

## Pull Request workflow

1. Push your branch: `git push -u origin feat/my-feature`
2. Open a Pull Request targeting **`master`**
3. Wait for review and approval (**required before merge**)
4. Merge via GitHub (squash or merge commit — team default: **squash**)

### If the PR has conflicts

Rebase onto latest `master` — do **not** merge `master` into your branch:

```bash
git checkout feat/my-feature
git fetch origin
git rebase origin/master
# resolve conflicts, then:
git add .
git rebase --continue
git push --force-with-lease origin feat/my-feature
```

## GitHub branch protection (admin setup)

Configure once per repository on GitHub:

**Settings → Branches → Add branch protection rule → Branch name: `master`**

Enable:

- [x] Require a pull request before merging
- [x] Require approvals (minimum: **1**)
- [x] Do not allow bypassing the above settings (applies to admins too)
- [x] Restrict pushes that create files matching the branch (or: block direct pushes)

Optional but recommended:

- [x] Require status checks to pass (CI workflow)
- [x] Require branches to be up to date before merging

## First-time repository bootstrap

1. Push `master` once to create the remote branch
2. Enable branch protection immediately (see above)
3. All further changes go through feature/fix branches and PRs
