# Enabling the Gravity Sandbox test suite in CI

`.github/workflows/build.yml` currently runs **only** `assembleDebug`, so none of the 62
Gravity Sandbox tests ever execute in CI. The previous forensic audit flagged this as a
structural false-confidence problem.

I prepared the change but **could not push it**: the GitHub App backing this session does not
hold the `workflows` permission, so GitHub rejects any commit that touches
`.github/workflows/`. Please apply this yourself — it is the single highest-value follow-up.

Insert these two steps immediately **before** the existing `Build APK with Gradle` step:

```yaml
      - name: Run JVM unit tests
        run: |
          if [ -f "./gradlew" ]; then
            ./gradlew testDebugUnitTest --no-daemon
          else
            gradle testDebugUnitTest --no-daemon
          fi

      - name: Upload test report
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: unit-test-report
          path: app/build/reports/tests/**
          if-no-files-found: warn
```

That makes `com.zig.gravity.GravityPhysicsTest`, `GravityCollisionTest` and
`GravitySandboxIntegrationTest` gate every push, which is what §3.17 ("MUST keep all 40 tests
passing") requires.
