# Catalog and personal data policy — local beta

Runtime reads normalized PostgreSQL rows from an explicitly activated snapshot. Repository JSON is an import artifact, never the public runtime database. P02's `PUBLISHED` state means immutable/sealed; V4 review plus activation determines public visibility.

## Review

1. Open official public documentation manually. Check the exact product, plan, country, channel, currency, billing period and feature. Do not infer native slide creation from text generation, or subscription quality from API benchmarks.
2. Write a short paraphrase, URL, publisher, claim type, UTC verification date, method, reviewer and conditions. No full-page copies, account data or source instructions. Reading a page verifies what the vendor states; it does not independently test quality or prove legal eligibility.
3. Use a separate evidence record for each important field. UNKNOWN has no asserted value: unknown price/quantity is null; unknown support is UNKNOWN, never UNSUPPORTED. A missing assertion also means unknown. Explicit absence needs evidence and precise scope.
4. Use CONDITIONAL when a fact depends on plan, rollout or account. Country-less USD list prices are conditional reference observations, not confirmed local checkout offers. KZ prices are UNKNOWN, not converted US prices. Tax and fees remain UNKNOWN unless separately established.
5. Keep model releases, products, agents, plans and regional offers separate. Model identifiers here name provider releases, not immutable internal weights; no benchmark values or quality scores are included.
6. Validate shape and semantics before import. Inspect the canonical content hash printed by import, review that exact hash, then activate with the current revision. No automatic review or publication on startup.

## Freshness

Public evidence contains both stored `status` and derived `effectiveStatus`. At the threshold, VERIFIED_SOURCE/CONDITIONAL becomes STALE: price/tax/fees 7 days; model/agent access and usage limits 14 days; other descriptive claims 30 days; FX 24 hours. UNKNOWN, CONFLICTED, DEMO and already STALE remain visible. Reads use the server UTC clock. The snapshot is not rewritten to mark ageing. This is read-time freshness, not a background source-fetch scheduler.

## Initial coverage

`data/catalog/catalog-2026-11-14-r2.json`: 3 products (ChatGPT, Claude, GitHub Copilot), 7 individual plans, 11 regional observations, 2 included agents (Codex, Claude Code), 2 named model releases. Coding has three products; web search has two scoped product assertions. This is a limited catalog, not a comprehensive comparison market.

Claude Pro's US monthly base price is explicitly supported by its help page; the annual charge comes from the pricing page and keeps separate conditions. OpenAI and GitHub USD list prices have unconfirmed regional applicability. No total checkout amount is claimed while taxes or fees are unknown. Free plans remain distinguishable from unknown prices. Copilot Student requires provider verification. Copilot Pro's shared credits are one pool, with variable flex allowance explicitly conditional.

No verified video-generation/design comparison, performance ranking, country-wide eligibility, FX, trial offer, or universal language/privacy compatibility is included. Native presentation export is explicitly UNKNOWN. Omitted candidates are coverage gaps, not evidence that those products lack capabilities. No paid APIs, account login, scraping job or automatic refresh runs.

## Corrections and rollback

Do not edit an imported snapshot, applied migrations or review history. Correct the manifest under a new UUID, validate/import/review/publish it. Rollback points to a previously activated, reviewed snapshot; its stored dates remain unchanged and may be stale. Stable entity IDs persist across versions; all joins include version_id. Public explicit-version reads accept only previously activated snapshots, never a merely imported or reviewed one.

Operators are local trusted users with project DB access; a CLI actor string is an audit label, not authenticated identity. Production least-privilege roles and authenticated admin workflows are later stages. No public write endpoint exists. External source pages remain untrusted content and cannot trigger actions.

## Benchmark and source usage rights

This catalog contains no benchmark dataset, score or copied source article. URLs and short factual paraphrases support individual claims; access to a public page does not grant a license to republish its contents. Before importing benchmark data, record its license/version, attribution, redistribution/commercial-use restrictions and method in source usage-rights notes. Unclear rights block ingestion; they are not assumed permissive. Keep API benchmarks separate from product/subscription quality and tie results to the measured model/version. Legal review and permission for new external collection remain separate gates.

## P08–P11 update: accounts, administration and local beta

The earlier P03 paragraph about the absence of a public write API described that stage. There is now a session/CSRF-protected ADMIN draft → validate → review → publish API. Ordinary users and guests cannot mutate catalogs. Publication uses the same immutable snapshots and active-pointer revision checks; admin audit contains identifiers/actions, not private profiles. External source fetching remains OFF. No new market assertions were invented for P08–P11; synthetic test catalogs are confined to isolated test databases.

Personal records include verified email/password hash, standard server sessions, saved needs/results/stacks, user-reported subscriptions, watches/notices, beta grants and a minimal grant audit. Price entries supplied by a user remain USER_REPORTED, not bank-confirmed. Reading a notification and applying it are separate actions; self-report is not a purchase or verified saving.

Free/Premium changes do not delete owned data. Full export and deletion remain available without Premium. A user-confirmed deletion revokes sessions and cascades owned records, including beta audit and analytics; catalog/audit actor references become null where appropriate. No payment records or future billing obligations exist in this beta. A future PSP requires a separately reviewed cancellation/financial-retention design.

Analytics is opt-in, account-linked (not anonymous), limited to three daily event presences: result saved, change notice acknowledged, consented account revisit. Server-confirmed saves/acknowledgements and a bounded revisit endpoint do not accept arbitrary event names or properties. Multiple actions on a day count once per event; these are user-days, not sessions, conversion, purchases or revenue. Guests are not tracked. Selection bias from opt-in users must be disclosed in any metrics. No external analytics service or payment event is present.

Events are query-visible for 30 UTC days; daily purge bounds physical retention to approximately 31 days while running. On restart, purge starts after one minute; reads never reveal expired days. Withdrawal deletes events immediately; account deletion deletes consent/events. Export contains the user's current consent and visible daily events, no other user's identifiers. No profile contents, email, IP, tokens, sources or subscription amounts are stored in analytics.

Local email remains a private development sink (directory 0700/files 0600) containing one-time secrets. It must not be copied to issue reports, screenshots or repositories. Working-mail cleanup/retention beyond existing account deletion and operator control is not production-ready. No real SMTP or notifications are sent. Monitoring outbox LOCAL_ONLY is not delivery confirmation.

Privacy, terms, tax and content-license compliance are **NOT_VERIFIED**; this document records implementation behavior, not legal approval. Closed-beta recruitment/data collection and public hosting require the owner's review and separate authorization.
