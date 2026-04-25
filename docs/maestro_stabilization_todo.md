# Maestro Stabilization TODO

## Rules
- [x] Follow the plan in `docs/maestro_stabilization_plan.md`
- [x] Save meaningful progress in this TODO file
- [x] Keep a running work log in `docs/maestro_stabilization_worklog.md`
- [x] Commit meaningful milestones as work progresses
- [x] Reflect durable learnings into memory when appropriate

## Phase 1. Current state re-check
- [x] Re-run Android smoke validation and capture exact current failure
- [x] Re-run iOS smoke validation and capture exact current state
- [x] Classify failures into runtime / Metro / Maestro selector categories

## Phase 2. Runtime stabilization
- [ ] Fix Android Metro/runtime readiness so RN red screen does not recur
- [ ] Fix Android launch flakiness if still present
- [ ] Fix iOS runtime issues if any remain
- [ ] Rebuild and relaunch both platforms after fixes

## Phase 3. Maestro stabilization
- [ ] Stabilize Android YAML first-screen assertions
- [ ] Stabilize Android mid-flow selectors and scrolling
- [ ] Stabilize iOS YAML assertions if needed
- [ ] Get one full Android pass
- [ ] Get one full iOS pass

## Phase 4. Repeated automation
- [ ] Run Android Maestro 10 times
- [ ] Run iOS Maestro 10 times
- [ ] Summarize pass/fail counts and recurring failure points

## Phase 5. Finalization
- [ ] Update final validation report
- [ ] Commit remaining meaningful changes
- [ ] Notify user with final outcome and caveats
