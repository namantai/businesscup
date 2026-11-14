# AI Compass — local beta release candidate

Status: **ACCEPTED for local beta demonstration**, owner decision 15.11.2026. P08/P10.a and P11 local QA are accepted. Independent review, coverage and all unexecuted production gates remain OPEN / NOT_RUN / NOT_VERIFIED. One local closeout commit is authorized; push/PR/merge/deployment and P12–P14 are not. See [DEMO_GUIDE](DEMO_GUIDE.md) for the demonstration.

## Stage and external-action scope

| Stage | Status | Scope |
|---|---|---|
| P07 | DONE, owner accepted | Existing subscription ledger/optimizer preserved |
| P08 | ACCEPTED, local beta | Protected catalog administration, bounded monitoring, in-app notices and local outbox |
| P09 | DEFERRED | Deterministic explanations suffice; no LLM SDK/API |
| P10.a | ACCEPTED, local beta | Free/Premium beta rights, expiry/revoke/audit, no payment |
| P10.b | DEFERRED | PSP/merchant eligibility and separate approval absent; no checkout/webhook/sandbox |
| P11 | Local QA ACCEPTED; remaining gates OPEN | Final local checks accepted by owner; independent review/coverage/production gates remain unexecuted |
| P12–P14 | NOT_STARTED | No staging, deployment or paid pilot |

All external integrations remain OFF. Beta fee is 0.00 USD. No real email, payment, paid AI API, web collector, deployment, remote CI, push or PR ran. No additional direct dependency was installed. The owner-supplied batch plan is preserved.

## T01–T38 traceability

Paths below are relative to `backend/src/test/java/com/aicompass/compass/` unless marked frontend. Backend/frontend rows are based on the P11 full check (156 unit +112 IT +74 UI PASS). The final full P11 browser run passed all 40 scenarios (14.1 min, exit 0). `RET` = `recommendation/RecommendationEngineTest`, `PIT` = `recommendation/ProfileAndInvariantTest`, `ID` = `identity/IdentityIT`, `CP` = `catalog/CatalogPublicationIT`, `OPT` = `recommendation/SubscriptionOptimizerTest`, `CH` = `recommendation/CatalogChangesTest`.

| ID | Evidence / exact scenario | Local status |
|---|---|---|
| T01 | RET.studentAndEditorHaveDifferentBundlesAndFreeWins: 3 needs/coverage; P05 E2E student flow | PASS |
| T02 | PIT.seededGeneratedProfilesNeverBreakBudgetRequiredFeaturesOrUniqueness; RET.unknownPriceAndTrialNeverBecomeFreeRecurring; P05 zero budget E2E | PASS |
| T03 | RET.unknownPriceAndTrialNeverBecomeFreeRecurring; MoneyTest FREE vs UNKNOWN; catalog/portfolio E2E | PASS |
| T04 | RET.studentSelfReportedVerificationStillConditionalAndNoneIneligible | PASS |
| T05 | RET.annualRequiresOptInAndEnoughUpfront | PASS |
| T06 | RET.mustKeepCountsCostAndCoverageExactlyOnce; OPT.mustKeepUnknownAndMissingCatalogNeverDisappear | PASS |
| T07 | RET.includedAgentAndSameVendorDoNotAddPurchasesOrBanBundles | PASS |
| T08 | Same RET case: compatible products with same vendor allowed | PASS |
| T09 | RET.maxToolsAndProductTierConflictsAreEnforced; mandatoryCountAndTwoProductTiersCannotBeSilentlyDropped | PASS |
| T10 | RET.countryAndPlatformAndPrivacyAreHard; P05 Linux/agent UI | PASS |
| T11 | RET.oneTimeCreditsAndUnverifiedFunctionCannotSatisfyMonthlyUsage | PASS |
| T12 | RET.sharedCreditsAreAllocatedOnceAcrossModes; OPT.requiredSharedQuotaPreventsFalseCheaperAlternative | PASS |
| T13 | RET.unverifiedTaxPreventsConfirmedBudgetFit; fxRequiresCurrentExplicitDirectionAndReturnsSnapshot; OPT.fxMissingCannotInventConversion | PASS |
| T14 | RET.outputLanguageIsNotInferredFromInterfaceLanguage | PASS |
| T15 | RET.countryAndPlatformAndPrivacyAreHard | PASS |
| T16 | PIT.staleAndConflictedSupportAreUnknownInsteadOfUnsupported; no confirmed winner with stale mandatory evidence | PASS |
| T17 | RET.noPublishedCatalogAndMissingTaxonomyAreHonest; partialIsNotHalfQualityAndRequiresExplicitConditionAcceptance; P05 no-match E2E | PASS |
| T18 | RET.studentAndEditorHaveDifferentBundlesAndFreeWins; optionalFeaturesSeparateStrategiesWithoutPaidBonus | PASS |
| T19 | RET.sameInputSnapshotAndTimeAreDeterministicAndStrategiesDoNotDuplicate | PASS |
| T20 | RET.candidateTruncationIsVisibleAndModelIdentityNeverInheritsQuality | PASS |
| T21 | RET.apiOnlyPricesCannotSubstituteForApplicationSubscriptions | PASS |
| T22 | RET.sameInputSnapshotAndTimeAreDeterministicAndStrategiesDoNotDuplicate; ID.snapshotsAreServerDerivedHistoricalOwnedAndDeletedWithAccount; explicit engine/policy compatibility | PASS |
| T23 | CP.readDuringSwitchNeverCombinesVersions; recommendation/RecommendationIT publication race; P08 CAS | PASS |
| T24 | RET.brandAndProviderNamesCannotChangeRanking; strict input rejects extra fields; no affiliate field/commission in ranking | PASS invariant; literal affiliate-toggle NOT_APPLICABLE (feature absent) |
| T25 | RET.paidCompassFeeIsIncludedInNetSavingsAndBudget; explicitFeeCurrencyUsesFxAndUnknownFxNeverBecomesZero | PASS |
| T26 | RET.annualCancellationDoesNotInventRefundOrImmediateSavings; OPT.prepaidAnnualIsSunkAndNoRefund; P07 E2E | PASS |
| T27 | Duplicate PSP webhook processing | DEFERRED with P10.b; no webhook/provider exists |
| T28 | ID.betaGrantAuthorizationCsrfStrictJsonAndFakeSuccessNeverGrantRights; P10 E2E fake success query | PASS |
| T29 | Out-of-order billing provider events / reconciliation | DEFERRED with P10.b; local beta CAS/replay is tested separately, not claimed as webhook validation |
| T30 | ID.monitoringCrashLeaseRecoveryRetriesAndFencesOldWorkers; monitoringOnlyImpactedOwnerGetsOneNoticeAndOutboxUnsubscribeIsConfirmed; P08 E2E | PASS |
| T31 | ID free profile/snapshot/subscription/notice/grant ownership; A/B E2E; analytics ownership | PASS |
| T32 | frontend Catalog.test untrusted markup; RET.sourceInstructionsCannotChangeDeterministicDecisions; no source fetch/LLM | PASS (P11 engine + UI) |
| T33 | RET.mandatoryCountAndTwoProductTiersCannotBeSilentlyDropped | PASS |
| T34 | RET.candidateTruncationIsVisibleAndModelIdentityNeverInheritsQuality; frontend limited real catalog/no video E2E | PASS |
| T35 | CH.metadataAndSourceTextDoNotNotify; small price/fee/FX threshold cases; budget crossing remains visible | PASS |
| T36 | config/IntegrationSettingsTest OFF enforcement + all guest engine/E2E flows without LLM | PASS; external LLM unavailable case DEFERRED P09 |
| T37 | ID.betaConcurrentGrantsReplayRevocationExpirationAndProfileLimits: delete beta grant/audit; account export/delete E2E; no future billing exists | PASS local beta; real PSP cancellation DEFERRED P10.b |
| T38 | CP.emptyDatabaseIsHonest; catalog/ScaffoldIT fresh Testcontainers; no demo seed in default runtime | PASS |

Coverage percentages are **NOT_RUN**, not inferred from test counts. JaCoCo/coverage instrumentation is not installed; adding/downloading it needs separate permission. Targets ≥85% decision branches / ≥70% backend are not claimed achieved. Tests include 160 seeded engine scenarios and 80 optimizer scenarios inside named tests; these iterations are not inflated into separate JUnit counts.

## Security and privacy review boundaries

Automated checks cover standard PostgreSQL sessions surviving backend restart, logout/reset/expiry, session fixation and CSRF rotation, owner/IDOR checks, strict JSON/mass assignment, persisted/concurrent rate limits, DB role checks, ADMIN CAS workflow, grant expiry/revoke/replay, Free/Premium direct API bypass, job fencing/dedup, opt-in/outbox/unsubscribe, no secrets in captured logs, private mail sink and default Secure cookies. Development uses loopback HTTP with Secure explicitly false; production defaults Secure and has no email transport.

P11 consent tests add collection OFF, owner isolation, concurrent withdrawal, daily dedup, retention, complete owner export and cascade delete. Only server-recorded saves/read acknowledgements and opt-in account revisit exist; no payment analytics, MRR or actual bank-confirmed saving is fabricated.

Self-review of changed boundaries was performed. **Independent read-only code review is NOT_RUN**; the owner explicitly accepted local QA while leaving this gate open. No subagent was launched. This and independent security/privacy/legal reviews remain owner gates, not successful automated tests. No claim of pentest or legal compliance.

## Financial demonstrations (synthetic, not market prices)

- Unit scenario: 12 + 8 + 5 USD/month → 300 USD over 12 months. A synthetic free bundle has an estimated 300 USD difference; `confirmedSavings=null`. User-reported conditions and unknown functional overlap remain explicit.
- Browser scenario edits the first entry 12 → 10: (10 + 8 + 5) × 12 = 276 USD. This is future expense arithmetic, not a refund or confirmed saving.
- Existing eligible creator plan at 1 USD/month → 12 USD horizon: KEEP_CURRENT, «Лучше ничего не менять».
- Unknown amount/cancellation/FX → UNKNOWN/null, not 0. Annual prepaid 120 is sunk cost, not an immediate refund. Included agents and shared quotas are not billed/countable twice.
- Hypothetical Compass fee 4.99 is tested in engine policy: a retained 12 USD plan becomes 16.99 all-in and −4.99 net monthly benefit. Actual local beta fee is 0.00; no hypothetical fee is presented as a charge.

## Local readiness matrix

| Gate | State / limit |
|---|---|
| Guest/catalog/accounts/optimizer | P11 backend/UI/E2E PASS |
| Admin → publish → impacted inbox | Real isolated E2E PASS; once per impacted watch/event |
| Free/Premium server rights | PASS including expiry/revoke/replay/concurrency/ownership |
| Analytics privacy controls | P11 PostgreSQL/UI/E2E PASS |
| Fresh migrations | V1–V11 clean PostgreSQL Testcontainers PASS, 45 tables; repeat migrate no-op |
| Supply chain | Local pinned/integrity/installed tree/offline Maven checks PASS; current CVE/provenance/license audit NOT_RUN |
| Mobile/keyboard | 360/768/1440 final P11 PASS; screenshots retained and representative views inspected |
| Persistence/ops | P11 stop/start persistence PASS; health/loopback PASS; backup/restore NOT_RUN |
| Catalog coverage | Limited: 3 real products, 7 plans, 11 regional observations; unknown taxes/local offers/limits remain visible |
| Browser environment | Existing approved Playwright Chromium with Ubuntu fallback on Arch; cross-browser/OS qualification NOT_RUN |
| Independent review / coverage | NOT_RUN independent review and coverage measurement; local owner acceptance does not close them |
| Privacy/legal/tax/terms | NOT_VERIFIED; documentation is implementation behavior, not approval |
| Remote CI / production | NOT_RUN; P12–P14 unauthorized |
| Real payments/emails/LLM/collectors | OFF / DEFERRED; no simulation represented as real revenue or delivery |

## Evidence and final handoff

P08: 155 unit +105 PostgreSQL IT +65 UI; 39 full E2E plus a final 1/1 focused replay (not 40 unique tests). P10: 155 unit +109 IT +71 UI +40 E2E, all mandatory commands exit 0. Full logs are local ignored `.runtime/p08-*` / `.runtime/p10-*`.

P11 final command results, screenshots, pre-closeout candidate paths and remaining gates are recorded below. The owner subsequently authorized one local closeout commit, including accepted P07 and the owner-supplied batch plan. Local `.env`, tools/cache/build/browser/mail/test artifacts stay ignored.

### P11 executed checks

| Command | Result |
|---|---|
| `bash scripts/check.sh` | PASS, exit0: 156 unit +112 PostgreSQL IT +74 UI; failures/errors/skips0; API drift/format/lint/typecheck/build/schema PASS |
| `bash scripts/check-supply-chain.sh` | PASS, exit0: 311 SHA512 lock entries, installed npm tree, offline resolved Maven tree, stable versions/pins |
| `bash scripts/check-catalog.sh` | PASS, exit0: synthetic valid +12 negative schema cases; both reviewed manifests and synthetic recommendation fixture |
| `bash scripts/smoke.sh` | PASS, exit0: readiness backend/frontend and frontend HTML, 3 checks |
| `bash scripts/check-secrets.sh` | PASS, exit0: candidate patterns/local credential scan and forbidden artifact paths; not a comprehensive detector |
| `bash scripts/status.sh` | PASS, exit0: PostgreSQL17.12 healthy, V1–V11 successful, catalog revision2, loopback5173/18080/15432 |
| `bash scripts/test-persistence.sh` | PASS, exit0: own probe survived stop/start; own probe table removed; volume/catalog retained |
| `bash scripts/benchmark-recommendation.sh` | PASS, exit0: 1 benchmark; p50=43.18ms, p95=51.66ms; 40 candidates/4 tools/20 measurements after5 warmups |
| `git diff --check` | PASS, exit0 |
| `bash scripts/test-e2e.sh` | PASS, exit0: 40/40 scenarios, 14.1 min; `.runtime/p11-e2e.log` |

Benchmark counts 102091 search nodes: 102090 non-empty combinations plus one empty root. The root is not recommended and is not a 102091st purchasable bundle. Performance is this local synthetic measurement, not a production SLO.

External source-down/redirect/SSRF collector scenarios are NOT_APPLICABLE to the implemented offline pipeline: no collector/fetch client is enabled and enabling source-fetch is rejected. Actual network-adapter testing is deferred with its authorization. LLM MOCK/provider failure tests are DEFERRED with P09; deterministic OFF behavior is tested. Current vulnerability feeds, package signature audits, remote CI and true backup/restore were not run.

### Historical Git candidate paths before final closeout

`codex/p01-scaffold`, HEAD `ffcde80860a3da71f28faa0bde29f056ab4dd348`; index empty. 84 modified/new paths including accepted P07 and the owner-supplied plan. No deleted path. Full `git status --short --untracked-files=all`:

```text
 M .github/workflows/ci.yml
 M README.md
 M backend/src/main/java/com/aicompass/compass/catalog/CatalogImportService.java
 M backend/src/main/java/com/aicompass/compass/catalog/CatalogPublicationService.java
 M backend/src/main/java/com/aicompass/compass/config/OpenApiConfiguration.java
 M backend/src/main/java/com/aicompass/compass/config/SecurityConfig.java
 M backend/src/main/java/com/aicompass/compass/identity/IdentityGuard.java
 M backend/src/main/java/com/aicompass/compass/identity/IdentityJson.java
 M backend/src/main/java/com/aicompass/compass/identity/IdentityService.java
 M backend/src/main/java/com/aicompass/compass/portfolio/PortfolioController.java
 M backend/src/main/java/com/aicompass/compass/portfolio/PortfolioDtos.java
 M backend/src/main/java/com/aicompass/compass/portfolio/PortfolioService.java
 M backend/src/main/java/com/aicompass/compass/recommendation/CandidateResolver.java
 M backend/src/main/java/com/aicompass/compass/recommendation/CostCalculator.java
 M backend/src/main/java/com/aicompass/compass/recommendation/PreviewLoadGuard.java
 M backend/src/main/java/com/aicompass/compass/recommendation/RecommendationEngine.java
 M backend/src/main/resources/application.yml
 M backend/src/test/java/com/aicompass/compass/ScaffoldIT.java
 M backend/src/test/java/com/aicompass/compass/catalog/CatalogIT.java
 M backend/src/test/java/com/aicompass/compass/catalog/CatalogPublicationIT.java
 M backend/src/test/java/com/aicompass/compass/identity/IdentityIT.java
 M backend/src/test/java/com/aicompass/compass/recommendation/RecommendationEngineTest.java
 M backend/src/test/java/com/aicompass/compass/recommendation/RecommendationIT.java
 M docs/ARCHITECTURE.md
 M docs/DATA_POLICY.md
 M docs/PROGRESS.md
 M docs/openapi.json
 M frontend/src/account/Account.test.tsx
 M frontend/src/account/AccountPage.tsx
 M frontend/src/account/Dashboard.tsx
 M frontend/src/account/SavePanel.tsx
 M frontend/src/api/account.ts
 M frontend/src/api/generated/index.ts
 M frontend/src/api/generated/sdk.gen.ts
 M frontend/src/api/generated/types.gen.ts
 M frontend/src/app/App.tsx
 M frontend/src/app/Navigation.tsx
 M frontend/src/catalog/Catalog.test.tsx
 M frontend/tests/e2e/zz-accounts.spec.ts
 M scripts/check-secrets.sh
 M scripts/e2e.py
 M scripts/test-e2e.sh
?? AI_COMPASS_BATCH_P08_P11.md
?? backend/src/main/java/com/aicompass/compass/analytics/AnalyticsController.java
?? backend/src/main/java/com/aicompass/compass/analytics/AnalyticsScheduling.java
?? backend/src/main/java/com/aicompass/compass/analytics/UsageAnalyticsService.java
?? backend/src/main/java/com/aicompass/compass/billing/EntitlementController.java
?? backend/src/main/java/com/aicompass/compass/billing/EntitlementService.java
?? backend/src/main/java/com/aicompass/compass/catalog/CatalogAdminController.java
?? backend/src/main/java/com/aicompass/compass/catalog/CatalogAdminService.java
?? backend/src/main/java/com/aicompass/compass/monitoring/CatalogChanges.java
?? backend/src/main/java/com/aicompass/compass/monitoring/MonitoringController.java
?? backend/src/main/java/com/aicompass/compass/monitoring/MonitoringRunner.java
?? backend/src/main/java/com/aicompass/compass/monitoring/MonitoringService.java
?? backend/src/main/java/com/aicompass/compass/portfolio/SubscriptionController.java
?? backend/src/main/java/com/aicompass/compass/portfolio/SubscriptionDtos.java
?? backend/src/main/java/com/aicompass/compass/portfolio/SubscriptionService.java
?? backend/src/main/java/com/aicompass/compass/recommendation/OptimizationResult.java
?? backend/src/main/java/com/aicompass/compass/recommendation/SubscriptionOptimizer.java
?? backend/src/main/java/com/aicompass/compass/recommendation/SubscriptionTerms.java
?? backend/src/main/resources/db/migration/V10__beta_entitlements.sql
?? backend/src/main/resources/db/migration/V11__consented_product_events.sql
?? backend/src/main/resources/db/migration/V7__subscription_ledger.sql
?? backend/src/main/resources/db/migration/V8__catalog_administration.sql
?? backend/src/main/resources/db/migration/V9__catalog_monitoring.sql
?? backend/src/test/java/com/aicompass/compass/recommendation/CatalogChangesTest.java
?? backend/src/test/java/com/aicompass/compass/recommendation/SubscriptionOptimizerTest.java
?? docs/RELEASE_CHECKLIST.md
?? docs/RUNBOOK.md
?? frontend/src/account/Analytics.test.tsx
?? frontend/src/account/AnalyticsPanel.tsx
?? frontend/src/billing/Billing.test.tsx
?? frontend/src/billing/BillingPage.tsx
?? frontend/src/monitoring/AdminPage.tsx
?? frontend/src/monitoring/InboxPage.tsx
?? frontend/src/monitoring/Monitoring.module.css
?? frontend/src/monitoring/Monitoring.test.tsx
?? frontend/src/subscriptions/Subscription.test.tsx
?? frontend/src/subscriptions/SubscriptionPage.tsx
?? frontend/src/subscriptions/form.ts
?? frontend/tests/e2e/zzz-subscriptions.spec.ts
?? frontend/tests/e2e/zzzz-monitoring.spec.ts
?? scripts/check-supply-chain.sh
?? scripts/status.sh
```


### Final P11 screenshots

Isolated synthetic fixtures, no real credentials/session identifiers. Browser assertions passed at 360/768/1440; account360, inbox1440, admin1440 and billing360 were also visually inspected. All files remain ignored test artifacts.

- [Catalog desktop](../.runtime/e2e-f25ab8dd505b/p05-catalog-1440.png)
- [Recommendations mobile](../.runtime/e2e-f25ab8dd505b/p05-results-360.png)
- [Subscription optimizer desktop](../.runtime/e2e-f25ab8dd505b/p07-subscriptions-1440.png)
- [Catalog administration desktop](../.runtime/e2e-f25ab8dd505b/p08-admin-1440.png)
- [Change notifications desktop](../.runtime/e2e-f25ab8dd505b/p08-inbox-1440.png)
- [Beta entitlements mobile](../.runtime/e2e-f25ab8dd505b/p10-billing-360.png)
- [Account and consent mobile](../.runtime/e2e-f25ab8dd505b/p06-account-360.png)

No remaining known application failure was observed in the executed suite. Independent review, coverage targets and external qualification gates above remain open; owner acceptance applies only to local beta and local QA, not every P11/production gate.


## Owner-approved local closeout — 15 November 2026

The owner accepted P08, P10.a and **P11 local QA only**, and authorized one local commit `feat: complete ai compass local beta candidate`. This supersedes historical no-commit statements above. Independent-review, coverage and production gates stay OPEN; no public launch is implied. P09/P10.b remain DEFERRED, P12–P14 NOT_STARTED.

- All original 84 candidate paths reviewed; all 37 original P07 paths present. No source/test/config/migration/dependency changes during closeout. Existing applied V1–V11 retained.
- Complete P11 suite reused: 156 unit +112 PostgreSQL IT +74 UI +40 E2E PASS, 0 failures/errors/skips. No new dependency/download/build required.
- Fresh status/smoke PASS (exit0, smoke3/3): PostgreSQL healthy, migrations1–11, catalog revision2, loopback5173/18080/15432.
- Fresh browser run on the working application PASS: homepage, real catalog3 products, two needs without prompt, live backend HTTP200/CONDITIONAL_ONLY with3 conditional alternatives and0 confirmed; explicit unknown-required-need request returned HTTP200/NO_MATCH with UNKNOWN and no confirmed bundles; missing-need UI data gap and beta pricing. No synthetic catalog was published into the working DB. Earlier temporary selector mistakes were corrected in the disposable probe, with no application change.
- New [DEMO_GUIDE](DEMO_GUIDE.md) separates live reviewed data from the isolated synthetic demo and explains startup/registration/portfolio/admin/rights/limitations. Candidate now85 paths (original84 plus guide); no secrets, mail, cache, logs or build artifacts are included.

Local acceptance does not close the open gates in the readiness matrix. Public release needs a separate authorized stage and operational/security/privacy readiness work; no P12 activity, push, PR, merge or deployment is part of this closeout.


Final closeout demo replay: **8/8 PASS, 8.1 min, exit0** with the exact filtered command in DEMO_GUIDE. Log `.runtime/closeout-demo-e2e.log`, screenshots `.runtime/e2e-a14693ac3b28/`. All eight requested demo groups were verified through the live working application and isolated real backend/PostgreSQL/browser scenarios; no static response substitutes or market claims from fixtures. This is a repeat of existing tests, not eight extra unique cases. No application defect/source change was found during closeout. Final secret scan and whitespace check PASS/exit0 before staging; exact85-path local commit uses `feat: complete ai compass local beta candidate`. Commit hash belongs to Git history/final handoff, not an extra self-referential documentation commit.
