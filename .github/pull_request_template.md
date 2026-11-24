## What & why
<!-- What does this PR change and why? Link the issue / defect. -->

## How it was tested
<!-- Commands you ran locally and their results. Example:
`./mvnw -pl 04-e2e-framework -am verify -Dheadless=true` → 24 passed / 0 failed -->

## QA checklist
- [ ] Tests added or updated for the change (positive **and** negative paths)
- [ ] New tests are deterministic: explicit waits only, no `Thread.sleep`, no order dependence, own test data
- [ ] Ran the affected module(s) locally and all non-`known-defect` tests pass
- [ ] Tests reproducing a real, unfixed defect are in group `known-defect` and link a defect report
- [ ] No test was disabled, deleted or moved to `quarantine` without an issue, owner and due date
- [ ] Test plan / test cases / README updated if scope or behaviour changed
- [ ] No secrets, tokens, real personal data or credentials in code, fixtures, logs or screenshots
- [ ] Tested only systems we own or are explicitly authorised to test
- [ ] Quality-gate thresholds (`10-quality-gate/quality-gate.properties`) unchanged, or the change is justified below

## Quality gate
The **Quality gate** check must be green before merging. If it fails, open the job summary (per-module table,
breaches, flaky list) and the `quality-gate-report` artifact (Allure).

## Risk / rollback
<!-- What could break? How do we roll back? -->
