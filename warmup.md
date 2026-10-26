1. `mvn test` failed: 80 tests ran, with 48 errors and no assertion failures.
2. The 45 controller tests errored while Spring was creating their test contexts.
3. Context creation failed because Mockito could not mock `TeamService`.
4. Mockito reported it could not modify `TeamService` and `Object`.
5. The controller tests failed before they could exercise their requests.
6. The run used Java 25, relevant to Mockito's inline-mocking setup.
7. Three business-rule tests errored while trying to create a team.
8. `TeamService.createTeam()` throws `UnsupportedOperationException`: creation is not implemented.
9. All 32 entity tests passed.
10. These failures explain the failed Maven test run.
