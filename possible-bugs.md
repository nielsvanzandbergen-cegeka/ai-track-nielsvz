# Possible bugs – PIT mutation report

Run: `mvn org.pitest:pitest-maven:mutationCoverage` (HTML report: `target/pit-reports/index.html`).
Scope: classes in `entity.*` and `service.*`, tested by the entity tests, `service.*` tests and
`RetroflowBusinessRulesTest`. Controller tests are excluded. PIT is configured in `pom.xml`.

**Result (PIT 1.30.1):** 123 mutants, 110 killed (89%), test strength 91%, line coverage of mutated classes 95%.
13 mutants are not killed: 11 survived and 2 have no coverage.

## Tooling issue found and fixed

The first run (PIT 1.17.1) reported 27 extra "NO_COVERAGE" mutants in `RetrospectiveService` (72% killed, 75% line coverage).
Cause: `RetrospectiveService` has `@Transactional` methods, so Spring generates a CGLIB proxy subclass at runtime. On Java 25 that
proxy has class file version 69, which PIT 1.17.1's bundled ASM cannot read
(`IllegalArgumentException: Unsupported class file major version 69` in `CoverageTransformer`). PIT then dropped coverage for the whole class.
The tests did cover the lines; the cause was Java 25 vs. the old PIT, triggered by the proxy.
Fix: upgrade `pitest-maven` to 1.30.1 and `pitest-junit5-plugin` to 1.2.3 in `pom.xml`. Those 27 mutants are now covered, and almost all are killed.

## Remaining non-killed mutants

| # | Location | Status | Meaning |
|---|---|---|---|
| 1 | `RetrospectiveService.java:117` | survived | Removing `requireOpen` in `addFeedbackItem` goes unnoticed. Either no service test adds ordinary feedback to a CLOSED retrospective, or the entity already enforces it and the service check is redundant. |
| 2 | `RetrospectiveService.java:142` | survived | Same for `addActionItem`. `addActionItem_whenRetrospectiveIsClosed...` passes without the check, which suggests the entity rejects the add too. Duplicated logic: confirm which layer owns the rule. |
| 3 | `RetrospectiveService.java:52` | survived (boundary) | No test uses a title of exactly 200 or 201 characters. |
| 4 | `RetrospectiveService.java:83,86` | no coverage | `listRetrospectives` has no service-level test (found, empty, missing team). |
| 5 | `TeamService.java:29` | survived (boundary) | No test uses a team name of exactly 200 or 201 characters. |
| 6 | `TeamService.java:33` | survived (boundary) | Same for member names. |
| 7 | `Retrospective.java:65-69` | survived (5 mutants) | No test asserts that the constructor sets title, date, status, team and an empty feedback list. |
| 8 | `Team.java:55` | survived | No test asserts that a new team has an initialised retrospective collection. |

## Suggested follow-ups

1. Add boundary tests (200 vs 201 characters) for team name, member names and retrospective title.
2. Add tests for `listRetrospectives` (found, empty history, missing team).
3. Add a test that adds ordinary feedback to a CLOSED retrospective; decide whether the service or the entity owns the rule and remove the duplicate.
4. Add constructor tests for `Retrospective` and `Team` asserting all initial state.
