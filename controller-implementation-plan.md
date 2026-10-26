# Implementation plan: REST controllers and controller tests

**Scope:** Expose every implemented service operation through REST controllers and cover each endpoint with controller tests.
Sources: `decisions.md` (brief + StackCraft standards), `prompt_log.md` (service plan, already implemented), `test-matrix.md`.
No code is written in this step.

## Current state

| Controller | Endpoint | Service call | Status |
|---|---|---|---|
| `TeamController` | `POST /api/teams` | `createTeam` | exists |
| `TeamController` | `GET /api/teams/{id}` | `getTeam` | exists |
| `RetrospectiveController` | `POST /api/teams/{teamId}/retrospectives` | `createRetrospective(teamId, title, date)` | exists |
| `RetrospectiveController` | `GET /api/teams/{teamId}/retrospectives` | `listRetrospectives` | exists |
| `RetrospectiveController` | `PUT /api/retrospectives/{id}/close` | `closeRetrospective` | exists |
| — | add feedback | `addFeedbackItem` | **missing** |
| — | add action item | `addActionItem` | **missing** |
| — | complete action item | `completeActionItem` | **missing** |
| — | uncomplete action item | `uncompleteActionItem` | **missing (decision required)** |

Existing tests: `TeamControllerTest`, `RetrospectiveControllerTest` and `ApiErrorHandlingTest` (45 tests, `@WebMvcTest` with `@MockBean` services via `ControllerTestSupport`). **None can run today** because Mockito cannot mock `TeamService` on Java 25.

## Step 0: unblock the controller test suite (prerequisite)

1. **Fix Mockito on Java 25.** Spring Boot 3.3.4 ships Mockito 5.11 / Byte Buddy 1.14.19, which do not support class file version 69. Override `byte-buddy.version` (and, if needed, `mockito.version`) in the `pom.xml` `<properties>` to a Java 25-capable release, and add `-XX:+EnableDynamicAgentLoading` to the Surefire `argLine`. The alternative is to run the build on JDK 21, which is the project's target (`java.version=21`).
2. **Re-run `mvn "-Dtest=TeamControllerTest,RetrospectiveControllerTest,ApiErrorHandlingTest" test`** to get a real baseline before adding endpoints.
3. **Resolve the ID-validation regression.** `@Positive` was replaced by `@NotNull` on path IDs. The existing tests expect `400` for `0` and `-1`. `decisions.md` §7 ("ID validation") lists zero/negative IDs as an open question. Either restore `@Positive` (matches the existing tests), or update those test cases to expect `404` after the product owner decides. **Decision required; do not change silently.**

## Step 1: request and response DTOs (`dto` package, records)

| DTO | Fields and boundary validation |
|---|---|
| `CreateFeedbackRequest` | `@NotBlank @Size(max=200) content`, `@NotBlank type` (`WENT_WELL` / `NEEDS_IMPROVEMENT`), `@NotBlank @Size(max=200) submittedBy` |
| `CreateActionItemRequest` | `@NotBlank @Size(max=200) content`, `@NotBlank priority` (`LOW`/`MEDIUM`/`HIGH`), `@NotBlank @Size(max=200) submittedBy`. No `completed` field: actions always start incomplete (`decisions.md` §5 open; the service already forces `false`). |
| `FeedbackResponse` | `id`, `retrospectiveId`, `content`, `submittedBy`, `type`, with a static `from(FeedbackItem)` |
| `ActionItemResponse` | the `FeedbackResponse` fields plus `priority` and `completed`, with a static `from(ActionItem)` |

- **Enum validation at the boundary.** The matrix (L106, L151) requires an invalid type or priority to return `400`. The service currently throws `RetroflowException` (`409`) for these, which is listed as a mismatch in `failed-test-matrix.md`. Add a custom constraint (e.g. `@OneOf` / `@ValueOfEnum`) on the request fields so the controller rejects invalid values with `400` before the service is called (`decisions.md`: "validate all API input at the boundary"). Keep the service check as a second line of defence.
- **Unknown JSON fields.** Reuse the existing `ApiJsonConfiguration` behaviour (FAIL_ON_UNKNOWN_PROPERTIES), already covered for `status` on retrospective creation (matrix L63).

## Step 2: `FeedbackController` (new; or extend `RetrospectiveController` if the 200-line limit allows)

| Method | Route | Service | Success |
|---|---|---|---|
| `addFeedbackItem` | `POST /api/retrospectives/{retrospectiveId}/feedback` | `addFeedbackItem(id, content, type, submittedBy)` | `201` + `Location: /api/feedback/{id}` + `FeedbackResponse` |
| `addActionItem` | `POST /api/retrospectives/{retrospectiveId}/action-items` | `addActionItem(id, content, priority, submittedBy)` | `201` + `Location: /api/action-items/{id}` + `ActionItemResponse` |

- Constructor injection only (standards 1.4). Each handler ≤ 20 lines (standards 1.3).
- Path IDs: same annotation as decided in Step 0.3.
- The `Location` header is built with `ServletUriComponentsBuilder.fromCurrentContextPath()`, like `createRetrospective`.

## Step 3: `ActionItemController` (new)

| Method | Route | Service | Success |
|---|---|---|---|
| `completeActionItem` | `PUT /api/action-items/{id}/complete` | `completeActionItem(id)` | `204` (update without body) |
| `uncompleteActionItem` | `PUT /api/action-items/{id}/uncomplete` | `uncompleteActionItem(id)` | always `409` for completed items |

- **Decision required:** `decisions.md` §5 asks whether an uncomplete endpoint should exist at all. Recommended: expose it so the irreversible-completion rule is observable over HTTP (matrix L162 → `409`). If the product owner says "no endpoint", skip it and rely on the service test.
- **Decision required:** whether completing after closure is allowed (`decisions.md` §5 "completion after closure"). The controller just delegates; the service owns the rule. Do not add a status check in the controller.

## Step 4: error handling (`GlobalExceptionHandler`)

No new exception types are needed. Verify that the new endpoints map correctly:
- `ResourceNotFoundException` → `404`
- `RetroflowException` → `409`
- `MethodArgumentNotValidException`, `HandlerMethodValidationException`, `HttpMessageNotReadableException`, `MethodArgumentTypeMismatchException` → `400`
- any other exception → `500`

All responses use the `ApiError(error, status, timestamp)` envelope and contain no internal class names (standards 2.2, 4.2). Add handlers only if a new boundary exception type surfaces (e.g. from the custom enum constraint).

## Step 5: controller tests

Follow the existing style: extend `ControllerTestSupport` (`@WebMvcTest`, mocked services, constructor injection), `methodName_condition_expectedBehaviour` names, Arrange/Act/Assert comments, named constants. Add `FeedbackController` and `ActionItemController` to the `@WebMvcTest({...})` list and add fixtures such as `FEEDBACK_JSON`, `ACTION_JSON` and `ACTION_ID`.

### `FeedbackControllerTest` (new)

| Test | Matrix |
|---|---|
| `addFeedbackItem_whenRequestIsValid_returns201WithLocationAndBody` | L100, L105 |
| `addFeedbackItem_whenRetrospectiveIsMissing_returns404WithError` (service throws not-found) | L101 |
| `addFeedbackItem_whenRetrospectiveIsClosed_returns409` (service throws `RetroflowException`) | L103 |
| `addFeedbackItem_whenTypeIsInvalid_returns400AndServiceIsNotCalled` | L106 |
| `addFeedbackItem_whenTypeIsActionItem_returns400` (generic feedback cannot create actions). Only add it once decided | L157 (DECISION REQUIRED) |
| `addFeedbackItem_whenContentOrSubmitterBlankOrTooLong_returns400` (one test per field) | L209 |
| `addFeedbackItem_whenBodyIsMalformedOrHasUnknownField_returns400` | L209, L219 |
| `addFeedbackItem_whenPathIdIsNotNumeric_returns400BeforeLookup` | L221 |
| `addActionItem_whenRequestIsValid_returns201WithPriorityAndNotCompleted` | L149, L150 |
| `addActionItem_whenPriorityIsInvalid_returns400AndServiceIsNotCalled` | L151, L210 |
| `addActionItem_whenCompletedFieldIsSupplied_returns400` (unknown field) | L154 |
| `addActionItem_whenRetrospectiveIsClosed_returns409` | L104 |
| `addActionItem_whenRetrospectiveIsMissing_returns404` | L101 |

### `ActionItemControllerTest` (new)

| Test | Matrix |
|---|---|
| `completeActionItem_whenActionExists_returns204` | L159 (response body is DECISION REQUIRED; `204` per standards 4.1 "update without body") |
| `completeActionItem_whenActionIsMissing_returns404WithError` | L160 |
| `completeActionItem_whenIdIsNotNumeric_returns400AndServiceIsNotCalled` | L213, L221 |
| `uncompleteActionItem_whenActionIsCompleted_returns409` | L162 |
| `uncompleteActionItem_whenActionIsMissing_returns404` | L167 |
| `uncompleteActionItem_whenIdIsNotNumeric_returns400` | L214 |

### Existing test classes to update

- `TeamControllerTest` and `RetrospectiveControllerTest`: only the `0` / `-1` ID cases, according to the Step 0.3 decision.
- `ApiErrorHandlingTest`: add one case per new endpoint for the `500` envelope (service throws `IllegalStateException`) and confirm no stack trace or package name is returned (L220).

### Optional end-to-end slice

One `@SpringBootTest` + `MockMvc` test (no mocks) for the full workflow: create team → create retro → add feedback → add action → close → add feedback `409` → complete action → uncomplete `409`. This guards the service/controller wiring that `@WebMvcTest` cannot see, and it does not depend on Mockito.

## Step 6: verification

1. `mvn "-Dtest=*ControllerTest,ApiErrorHandlingTest" test`, which should be all green.
2. `mvn test`, the full suite including the service, entity and business-rule tests.
3. `mvn org.pitest:pitest-maven:mutationCoverage`. Optionally add `controller.*` to PIT `targetClasses` and the controller tests to `targetTests` once Mockito works.
4. Check coverage against the standards minimum (80% line / 70% branch, `decisions.md`).
5. Update `README.md` with the endpoint list (standards 6.2) and the `failed-test-matrix.md` rows that the new endpoints make testable (106, 151, 154, 209-214).

## Decisions required before or during implementation

| # | Decision | Affects |
|---|---|---|
| 1 | Zero/negative IDs: `400` or `404` (`@Positive` vs `@NotNull`) | all path IDs, existing tests |
| 2 | Expose `uncomplete` endpoint or not | `ActionItemController` |
| 3 | Completion allowed after retrospective closure | service rule; `complete` tests |
| 4 | Feedback edit/delete endpoints in scope | matrix L124-135; possible `PUT`/`DELETE /api/feedback/{id}` |
| 5 | Action-item filtering (`GET /api/action-items?priority=&completed=`) scope | matrix L181-194 |
| 6 | Feature-first package migration (`team`, `retrospective`, `feedback`) or grandfathered layer packages | location of the new controllers |
