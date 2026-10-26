# Implementation plan: `RetroflowBusinessRulesTest`

**Scope:** Make the three tests in `RetroflowBusinessRulesTest` pass without changing their assertions or disabling them. Implement business rules in the service layer and persist changes through the existing Spring Data repositories and H2 database.

1. **Use the business-rule tests as the acceptance criteria.** Run `mvn -Dtest=RetroflowBusinessRulesTest test` to capture the current baseline. Preserve all three tests: adding feedback after closure must throw `RetroflowException`; a team must not have two open retrospectives; and a completed action item must not be uncompleted.
2. **Wire the services to persistence.** Add constructor-injected `TeamRepository`, `RetrospectiveRepository`, and `FeedbackItemRepository` dependencies to the services that need them. Keep service operations transactional so entity changes and status transitions are saved consistently.
3. **Implement `TeamService.createTeam(String, List<String>)`.** Validate the required team name and nonempty member list, create and save the team, and return the persisted entity. Throw a domain exception using the established `EntityType identifier: reason` format for invalid input; the test's valid team data must succeed.
4. **Implement both `RetrospectiveService.createRetrospective` overloads.** The test invokes `createRetrospective(teamId, title)`, so this overload must work as well as the explicit-date overload. Resolve the team or throw `ResourceNotFoundException`, initialize the retrospective as `OPEN`, and save it. Before saving, reject creation with `RetroflowException` if that team already has an open retrospective; add the repository lookup needed to check by team and status. **Decision made:** the two-argument overload uses the current date because the product documents do not specify a default.
5. **Implement `RetrospectiveService.closeRetrospective(Long)`.** Load the retrospective or throw `ResourceNotFoundException`, transition it to `CLOSED`, and persist the change. Preserve the entity's irreversible `CLOSED` state; do not add closure prerequisites that the requirements have not settled.
6. **Implement `RetrospectiveService.addFeedbackItem(Long, String, String, String)`.** Resolve the parent retrospective and check that it is `OPEN` before creating feedback. For a closed retrospective, throw `RetroflowException` without saving or attaching an item. For an open retrospective, parse the requested feedback type, create the matching supported feedback subtype, and persist it; reject unsupported types with a domain validation exception.
7. **Implement `RetrospectiveService.addActionItem(Long, String, String, String)`.** Resolve the parent and require it to be `OPEN`; parse the priority into `Priority`, construct an `ActionItem` with type `ACTION_ITEM` and its initial `completed` value false, then save it through `FeedbackItemRepository`. Reject invalid priorities and missing retrospectives with the appropriate domain exceptions.
8. **Implement `RetrospectiveService.completeActionItem(Long)`.** Find the requested action item or throw `ResourceNotFoundException`, transition it to completed, and persist the change. Preserve the action's irreversible-completion rule.
9. **Implement `RetrospectiveService.uncompleteActionItem(Long)`.** Find the action item or throw `ResourceNotFoundException`. If it is completed, throw `RetroflowException` and leave it completed; never persist a reversal. This is the direct behavior asserted by `completedActionItem_cannotBeUncompleted`.
10. **Re-run the focused acceptance test, then the full Maven suite.** Confirm all three business-rule tests pass and the state changes persist. If unrelated controller tests still fail, report those separately rather than weakening or changing the business-rule tests.

## Service methods currently unimplemented

- `TeamService.createTeam`
- `RetrospectiveService.createRetrospective(Long, String, LocalDate)`
- `RetrospectiveService.createRetrospective(Long, String)`
- `RetrospectiveService.closeRetrospective`
- `RetrospectiveService.addFeedbackItem`
- `RetrospectiveService.addActionItem`
- `RetrospectiveService.completeActionItem`
- `RetrospectiveService.uncompleteActionItem`
