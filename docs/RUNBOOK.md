# AI Compass — local beta runbook

Scope: local AI Compass only. Not a production deployment guide. All commands below work from fish by explicitly invoking Bash. Do not run sudo, Docker prune, volume deletion, destructive Git commands or global configuration changes.

## Start, stop and inspect

From the repository root, with the already approved project tools/dependencies present:

```bash
bash scripts/check.sh
bash scripts/check-supply-chain.sh
bash scripts/dev-up.sh --no-build
bash scripts/status.sh
bash scripts/smoke.sh
```

`check-supply-chain.sh` requires the already cached `maven-dependency-plugin:3.10.0` and intentionally runs offline. On a fresh cache it fails closed instead of downloading a tool; request approval for that exact project-local tool before obtaining it. No current CVE database or signature verification is implied.

If the application is already running, first use `bash scripts/dev-down.sh`.
`dev-down.sh` stops only recorded AI Compass processes and the named Compose project, retaining the PostgreSQL volume. `dev-up.sh` checks listeners and refuses occupied ports; it does not kill unrelated processes. Defaults are loopback frontend 5173, backend 18080, PostgreSQL 15432. `.env` is private/ignored; `.env.example` contains no real credential. Never print Compose's expanded configuration or copy local mail/credentials into reports.

`bash scripts/test-persistence.sh` writes an isolated probe row, stops/starts this project's services, verifies it and removes its own probe table. It is a persistence check, **not** a backup/restore test. If interrupted, inspect the probe and process state; never delete the volume to repair it.

## Catalog and administrator

1. Register and verify a local account with the existing private dev mail sink.
2. Initial administrator bootstrap uses `bash scripts/admin-bootstrap.sh <verified-account-UUID>` (see script usage). It is one-time; do not promote arbitrary accounts or share credentials. Bootstrap was integration-tested; no working user was promoted during the batch.
3. Open `/admin/catalog`. Save a new manifest draft, validate, review the sealed content and publish against the displayed active revision. Invalid/unreviewed/stale revision operations fail. Editing invalidates review. Published snapshots remain immutable.
4. Sources, verified date, conditions and UNKNOWN/STALE status are reviewed manually. The prototype and test fixture prices are not market facts. No scraper or URL fetcher is enabled.
5. Publication conflicts require reload and a new deliberate decision. Never modify catalog tables directly or rewrite migration checksums.
6. For reviewed historical rollback, inspect `bash scripts/catalog.sh` usage and the publication history; activate a previously published snapshot with the current expected revision. Dates remain historical; rollback does not freshen evidence. Local CLI actor labels are not authenticated identity; use the protected UI for normal publication.

A single trusted ADMIN can validate/review/publish. This is an audited workflow, not an enforced two-person approval policy. Independent source review remains an operational responsibility.

## Monitoring and incidents

The append-only catalog publication log is the event source. Watch/revision uniqueness deduplicates work; jobs have leases, fencing tokens and at most five attempts, with delayed retry. Only relevant semantic changes trigger recomputation. Price thresholds do not hide eligibility/budget changes. UNKNOWN is never a zero-cost improvement.

`bash scripts/status.sh` shows aggregate job/outbox states, current catalog revision, migrations, health and project listeners without personal payloads. A FAILED job requires inspection of the cause by the operator; do not repeatedly republish the same data or reset attempts blindly. There is no automated operations alert service and no admin job-retry button. The local scheduler processes at most one job per minute and throttles each watch; this is suitable for a small beta, not a claimed high-volume service.

On worker interruption, restart normally: expired leases are reclaimed; stale workers cannot duplicate notices. On consent withdrawal, pending local email outbox records are cancelled. Outbox state LOCAL_ONLY never means delivered email. There is no network sender. Unsubscribe is confirmed; applying a recommendation is self-reported only.

Monitoring creation/enabling/recomputation requires an active Premium beta grant. Expiry pauses effective monitoring; saved messages and confirmed unsubscribe remain available. User data are preserved until explicit deletion. Renewed access may process relevant queued publication history within the same bounded scheduler.

## Beta rights and privacy

`/pricing` lists actual server limits; `/account/billing` shows current rights and history. ADMIN can issue 1–90 days or revoke access with explicit confirmation and revision checks. A request UUID is an idempotency key. Replay cannot resurrect a revoked grant. No payment, card input, automatic renewal or future charge exists. Actual beta fee is 0.00 USD. P10.b is DEFERRED pending provider/merchant eligibility and separate authorization.

Optional product analytics is OFF until explicit account consent. It stores only a daily presence of RESULT_SAVED, CHANGE_NOTICE_VIEWED (explicit acknowledgement) and REVISIT (consented account screen visit), linked to the account. No free-form properties, email, profile content or IP are stored in analytics. The rolling window is 30 UTC days; a daily purge runs while the application is up (physical retention can reach 31 days between sweeps). Queries exclude older data immediately. Withdrawal and account deletion remove events immediately and serialize with concurrent collection.

`/account` exports all owned profiles, snapshots, stacks, subscriptions, watches/notices, beta rights/audit and analytics. Export is not truncated to the inbox's 100-row view or Free's current-result view. Password-confirmed deletion revokes sessions and removes owned data and local mail. Catalog audit may retain an actor-null record; it must not retain email or profile payloads. See DATA_POLICY for limits and the required independent privacy/legal review.

## Recovery boundaries and staging gates

- A failed migration: stop application changes, retain database/volume and inspect the exact error. Never edit V1–V11 after application; add a forward migration after review.
- A catalog regression: use reviewed publication rollback, not SQL deletion.
- An application regression: retain the database and review application/schema compatibility before switching an artifact. Automated application rollback and real backup/restore are NOT_RUN here.
- A suspected credential leak: stop affected local sessions, rotate only through an authorized workflow, retain a redacted incident record; do not attach secret-bearing logs.
- Before P12: owner permission, hosting budget/region, HTTPS same-origin proxy, least-privilege database roles, reviewed secrets/backup/restore, retention and source-owner responsibility, legal/privacy review, independent security review, dependency vulnerability review and a tested supported browser environment are required.

Production cookies default Secure/HttpOnly/SameSite=Lax. The development script explicitly disables Secure for loopback HTTP; this is not a production recommendation. Real email delivery is unavailable in production mode. LLM, payments and source fetch are OFF and unsupported modes fail startup. No staging/production action is authorized by this runbook.
