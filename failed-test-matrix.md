# Matrix rows without a test

Rows marked DECISION REQUIRED in `test-matrix.md` are intentionally ignored.
The rows below are decided but could not be tested; no source code was changed to force them.

## Now covered by controller tests (previously listed here)

| Matrix line | Covered by |
|---|---|
| 106 | `FeedbackControllerTest.addFeedbackItem_whenTypeIsInvalid_returnsBadRequestBeforeService` (400 at the boundary) |
| 151 | `FeedbackControllerTest.addActionItem_whenPriorityIsInvalid_returnsBadRequestBeforeService` (400 at the boundary) |
| 154 | `FeedbackControllerTest.addActionItem_whenBodyIsInvalid_returnsBadRequestBeforeService` (`completed` is an unknown field, 400) |
| 209, 210 | `FeedbackControllerTest` invalid-body / malformed-id tests |
| 213, 214 | `ActionItemControllerTest.actionItemEndpoints_whenIdIsMalformed_returnBadRequestBeforeService` |

The service still throws `RetroflowException` (409) for an invalid type/priority when it is called directly; over HTTP the boundary rejects it first with 400.

## No operation exists to exercise the rule

| Matrix line | Rule | Missing operation |
|---|---|---|
| 87, 155 | general update with invalid status / boolean | general update of a retrospective or action item |
| 102 | feedback without a parent retrospective | no API to create feedback without a parent |
| 125, 129 | delete feedback / action in a closed retrospective | delete feedback |
| 132, 133 | modify / delete a missing feedback item | modify and delete feedback |
| 135 | change feedback type to an invalid value | change feedback type |
| 152 | modify priority to an invalid value | modify action priority |
| 181-184, 192 | completed filter, invalid filters, empty filtered result | list/filter endpoints and service methods |
| 211, 212 | boundary validation for modify / delete feedback | modify and delete feedback |
