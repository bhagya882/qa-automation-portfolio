# Defect Report Template (Jira / GitHub Issues)

Copy it into a Jira "Bug" issue or a GitHub issue. A GitHub issue form version is in
[github-issue-form-defect.yml](github-issue-form-defect.yml). Copy that file to `.github/ISSUE_TEMPLATE/defect.yml` to enable it.

```markdown
**ID:** DEF-<project>-<nnn>                **Reported by / date:** <name>, YYYY-MM-DD
**Title:** <component>: <what is wrong> (<when / for whom>)
**Severity:** Blocker | Critical | Major | Minor | Trivial
**Priority:** P1 | P2 | P3 | P4
**Status:** Open | In progress | Fixed | Retest | Closed | Rejected
**Linked test case(s):** TC-…

### Environment
- URL / build:
- Browser + version:            - OS / device:
- Test data / account (synthetic or public demo only, never real credentials):

### Preconditions
…

### Steps to reproduce
1. …
2. …

### Expected result
…

### Actual result
…

### Evidence
Screenshot / video / console log / HAR / response body (links). Redact tokens and cookies.

### Frequency
Always | Intermittent (x of y) | Once

### Notes / suspected cause / workaround
…
```

## Severity definitions
| Severity | Meaning | Example |
|---|---|---|
| Blocker | Core journey impossible, no workaround | Cannot finish checkout |
| Critical | Core function wrong: data loss, money or security impact | Wrong product or price in cart |
| Major | Important function broken, a workaround exists | Sort by price doesn't work |
| Minor | Cosmetic or UX issue, low impact | Missing empty-state message |
| Trivial | Typo, alignment | |

## Priority definitions
P1 = fix before release · P2 = fix in the next sprint · P3 = backlog · P4 = nice to have.
