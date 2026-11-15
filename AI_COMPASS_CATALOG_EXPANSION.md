# AI COMPASS — CATALOG EXPANSION (CE01–CE04)

**Execution plan for Codex**
**Status on handoff:** Local beta (P01–P07 DONE; P08 and P10.a accepted for local beta; P09/P10.b deferred; P12–P14 not started). Latest owner-reported commit `b9256e09c4bb1d241d25186fbc53bd64552684bf`, branch `codex/p01-scaffold`. Verify Git state; do not assume it is unchanged.
**Goal:** Expand the existing small, source-reviewed catalog of 3 AI products to a useful, multi-category catalog of **20+ genuine AI products**, including free plans, agents and Chinese AI services; ensure the existing Recommendation Engine can choose defensible multi-product bundles for students, software developers, researchers and creative professionals without the user entering a prompt.

## 0. Permanent rules and scope

Read completely before editing: `AGENTS.md`, `CODEX_PROMPT.md`, `docs/PROGRESS.md`, `docs/ARCHITECTURE.md`, `docs/DATA_POLICY.md`, `docs/RELEASE_CHECKLIST.md`, `docs/DEMO_GUIDE.md`, this file, and relevant catalog schemas/manifests/validators. The current product architecture and security guarantees take precedence. All code is written by Codex. Do not change applied Flyway migrations V1–V11, overwrite the existing immutable published catalog, or modify the approved production-like dataset in place. No unrelated feature development.

This is a **catalog / quality of recommendation expansion**, NOT P12 deployment or P13 payments. Keep `LLM_MODE=OFF`, payments OFF, real email OFF, source-fetch collectors OFF. No external paid model calls; no third-party logins, scraping bypasses or account purchases; no git push, PR, deployment, paid services or global installs. New direct dependencies need owner approval before installation. No commit without owner approval. User's existing app/DB/dev data must not be destroyed; use isolated temp DB for disruptive tests. Follow AGENTS.md security and Git rules.

Browsing **public official documentation and pricing pages for manual fact-checking** is in scope. Do not start automated high-volume crawling, bypass robots/captcha/auth, or use unauthorized benchmark datasets. If network or evidence is unavailable, do not fabricate facts; mark unknown, defer that field/product and report the precise blocker.

## 1. Core product definition — don't drift

The customer chooses **several recurring needs** (e.g. learning, coding, presentations; or image, video, editing and audio), importance/mandatory features, country, language, operating platform, use intensity, budget, upfront budget and existing subscriptions. The customer does **not** need to submit an individual coding task/prompt. The Java Recommendation Engine returns zero to three **defensible, possibly identical-strategy** bundles and explanations based on verifiable capabilities, eligibility, prices, quotas and evidence. It is a deterministic **recommender/optimizer**, not a new live LLM judging other LLMs. A separate LLM explainer remains optional and OFF.

Do not imply that a model family, its API, an end-user application, a coding agent and a subscription plan are the same entity. Do not rank price as model intelligence. Do not call capability coverage a benchmark quality score.

## 2. Product target and priorities

Expand to target **23 products including the 3 existing**, preserving existing records where correct. Suggested candidates, **not claims of verified tariffs**:

| Priority | Category | Products / product families to investigate |
|---|---|---|
| A | General AI, education, writing | ChatGPT, Claude, Gemini, DeepSeek, Qwen, Kimi |
| A | Coding tools and agents | GitHub Copilot, Cursor; assess separate Codex and Claude Code agents as *included agents* where appropriate, not automatically duplicate paid products |
| A | Research, slides, design | Perplexity, Canva |
| A | Image and video creation | Higgsfield, Runway, Leonardo AI, Ideogram, Midjourney |
| A | Audio/music | ElevenLabs, Suno |
| B | Video editing | CapCut, Descript |
| B | Additional video-generation choices | Kling AI, Hailuo AI, PixVerse, Luma Dream Machine |

This table identifies ~23 separate products depending on verified product boundaries. If a named item is actually a model inside another platform, use `ai_model`/access relations instead of duplicating a purchasable subscription. Check official identity and site. Do **not** require all candidates to have paid offers or confirmed regional prices. An honest limited catalog is better than guessed data.

**Targets** (record actual result, never claim unmet targets): >=20 product listings with verified identities and official links; >=12 with evidence-backed plan details; >=8 verifiably free tiers **only where official source confirms actual recurring free use**; coverage of at least 6 useful categories; at least one evidence-backed image product, video product, and audio product; at least three real end-user scenarios that produce meaningful confirmed/conditional matches. Targets are acceptance criteria only to the extent supported by real official sources; if impossible, report partial completion rather than manufacture data.

## 3. Verified sources, evidence and data policy (CE01)

For each product research its **official website, official pricing page, plan FAQ/help, terms of use and relevant usage/credit documentation**. Capture a source URL, exact product/plan, observed timestamp, geographic context, billing channel, and direct evidence per **claim**. Prefer public vendor pages over aggregators. Do not infer price from a homepage, third-party blog or search snippet alone. Review sources as of current date at execution; don't assume previously quoted pricing is still valid.

Store evidence for separate questions:
- Is the product an app, paid subscription, model/agent, API-only offer, credit pack, or a platform with bundled models?
- Does a truly free **recurring** tier exist? Distinguish limited free use vs free trial, expiring signup credits, coupon, daily renewal, account-personalized promotion, annual-only discount and usage-based API pricing.
- Which functions/needs are explicitly provided and through which **plan**? Classify supported/unsupported/conditional/conflicted/stale/unknown using current semantics.
- Price: exact amount, currency, region, purchase platform, invoice interval, upfront charge, full checkout/tax/fee if substantiated, and whether discounts are time-limited. Store a **yearly billed price** with full upfront, not the monthly equivalent as immediately payable.
- Credits and quotas: cadence, pool scope, replenishment, watermark, generation resolution/duration, model eligibility, commercial rights only where documented; note specific conditions.
- The same model may be available on different products and plans; link exact model/access instead of copying quality claims to the host app.
- Languages, web/mobile/desktop/IDE compatibility, student eligibility, privacy/local processing: unknown unless proven.

**IMPORTANT Higgsfield caution:** its official `higgsfield.ai/pricing` page and product help/pricing explanations may show different plan names, paid billing options, campaigns and credit counts. Record only an unambiguous matched billing state with source and date; if the figures conflict, use `CONFLICTED`/`UNKNOWN` and preserve that fact. Its free tier must not be mistaken for renewable monthly credits (official free-plan FAQ says no monthly allowance). Do not claim that multiple bundled models imply independent subscriptions.

For Kazakhstan and other regions: an official USD list price is **NOT** a verified Kazakhstan checkout amount or authorization to purchase from another country. Store KZ taxes, total and eligibility UNKNOWN until supported. No VPN workaround recommendations. If a plan is simply not priced for KZ, show UNKNOWN, not FREE or USD=KZT.

Do not import benchmark quality numbers from third parties without verifying methodology, exact **model version**, date, licensing and applicability. If there is no qualified evidence, keep quality UNKNOWN. Capability support is not proof that one provider is *better* at that task.

Build an evidence review matrix in `docs/CATALOG_EXPANSION_REVIEW.md`: product, plan/agent/model, claim, official link, last checked, supported region/channel, price type, evidence status, reviewer notes, conflict notes. Do not put unverified content into a production-reviewed snapshot. Quote/paraphrase sparingly and comply with data source terms.

## 4. Data preparation, review and publication (CE02)

1. Inspect current V1–V11 schema, current `catalog` import/review/publish commands, manifest JSON schema and **active published snapshot UUID**. Record it before changes.
2. Build a **new versioned manifest** by extending the accepted r2 information conservatively. Preserve unchanged reviewed evidence and historical timestamps; refresh only claims actually rechecked.
3. Add relevant `user_need`, `capability`, `need_capability`, `capability_assertion`, `eligibility_rule`, `quota_pool`, `usage_limit`, `plan_model_access`, `plan_agent_access`, `evidence_source`, `evidence`, `price_offer` using established contracts. Design needs for study, coding, presentations, research, documentation, writing, images, video generation, video editing, design, audio/voice, music, agents, and data analysis where verifiable.
4. Avoid duplicating model/provider/product and plan IDs. Do not equate API/token pricing to plan monthly subscription. Qualifiers for annual billing, subscription credits and limited free tiers must be explicit.
5. Validate JSON Schema + semantic rules, conflicting statements, references and catalog policy. Test stage/import -> sealed draft -> review -> publish under existing admin/security workflow. **Do not publish to the actual dev active catalog until reviewed**, even for local use; run end-to-end in isolated test DB first. If approved owner instruction allows dev publication after evidence review, take record of previous pointer and confirm rollback method, then publish exactly one new immutable reviewed snapshot. Never publish synthetic fixture as real market data.
6. Ensure old r2 links/history still resolve and rollback works; no destructive cleanup. Verify read APIs use a single pinned catalog version.

If data source conflicts cannot be resolved, retain listing with unknown plan/price and exclude it from confirmed budget-fit and automated buyability. Report coverage rather than fill with fake data.

## 5. Recommendation impact and UI (CE03)

Reuse existing Java Recommendation Engine. **Do not implement a second recommender or add paid bias**. Make only necessary catalog / taxonomy / UI wiring fixes, with a regression test for each. Existing P05/P07/P08/P10 flows must remain intact.

On `/onboarding`, categories come from the published backend taxonomy (no hardcoded pseudo-categories). The user should be able to select multiple needs such as `study+coding+presentations` and `image+video+editing+voice`. Clearly identify mandatory vs optional needs. If confirmed coverage is insufficient, present NO_MATCH/CONDITIONAL with precise reasons, rather than suggest a randomly chosen AI.

On `/catalog`, offer product type/category filters, search, free-vs-paid (only if evidence-backed), price-currency/region status, product detail and sources. On `/results`, preserve correctly labeled Economy/Balanced/Expanded when distinct; show real supported capabilities, covered/unmet requirements, plan/agent inclusion, full upfront, monthly equivalent, billing terms, credits, uncertain region/tax/quality and source links. Display an explicit limited-market disclaimer. Always show why certain products were excluded (optional user-facing summary), with no unsupported ratings/percentages.

Ensure various needs lead to potentially different outcomes. For example: a student who needs study+code+presentations (with student verification conditional), a creator who needs image/video/editing (credits and commercial rights), and a researcher who needs source-backed research. **Model results must be computed from actual catalog data**; expected labels are not hardcoded into UI, tests or production.

If a user selects a budget in USD but a plan is in CNY/other currency, do not convert without accepted FX snapshot; show comparison limitation. Existing entitlement and subscription logic must still handle newly loaded product IDs without ownership exposure or broken migrations.

## 6. Test matrix and integrity gates (CE04)

Use `Testcontainers` PostgreSQL and isolated browser E2E dataset; preserve the user's actual dev DB/catalog/accounts. Validate at minimum:
- Schema and semantic manifest validity, duplicate IDs, cross-version FK/reference checks, evidence quality, distinct FREE/UNKNOWN/trial/one-time credits, annual upfront, monthly equivalent and currency; exact data does not silently change old r2 history.
- Published A -> new B -> public API/React -> rollback to A; version-pinned product links; concurrent publication/review remains atomic.
- Search/filter/list/detail for at least one product from every category; images/video/audio offerings and included agents; no synthetic data in production build or live snapshot.
- Profile student (multiple needs), creator (video/image/editing), researcher (sources), zero-budget users, free-only available bundle, conditional/no-match, missing FX, KZ unknown price, too-small budget, confirmed hard constraints, credits/quotas and bundles under 4 tools; costs remain accurate.
- Existing subscriptions P07, no fabricated savings, mustKeep, monitoring events P08 only for genuine semantic changes, beta entitlements P10.a, auth/ownership, CSRF, rate limiting, export/delete.
- No live AI API, no real emails, payments, URL collectors, public deployment or accidental external calls.
- Browser 360/768/1440, keyboard/focus, loading/empty/error, browser console and no horizontal overflow. For a verified claim, source URL must display; UNKNOWN is not FREE.

Run established scripts in sequence as applicable: `bash scripts/check-catalog.sh`, `bash scripts/check.sh`, `bash scripts/smoke.sh`, `bash scripts/test-e2e.sh`, `bash scripts/check-secrets.sh`, `git diff --check`, plus local status/persistence if catalog publication could affect them. Record raw real pass/fail/skip numbers and regression-before/fix-after in `docs/PROGRESS.md`. Tests must not silently use mock pricing in live catalog. No test counts invented from loop iterations.

## 7. Git / rollback / delivery

Operate in existing branch (verify `codex/p01-scaffold` and HEAD first). Keep old records and previously accepted release commit intact. Do not edit applied Flyway migrations. No local commit/staging without approval; no push, PR, merge or deployment. Exclude `.env`, mail, caches, database dump, logs, node_modules, generated screenshots and unrelated temp files. Capture current active snapshot and how to revert it. Preserve server origin loopback and integration OFF defaults. Do not claim independent audit, legal compliance or benchmark quality verified if not performed.

Report:
1. Product count, accepted plans/offers, free plans, agents/models, categories; source coverage and exact missing facts.
2. Actual names of added products, NOT just proposed list; official URLs and verification status, conflicted/unknown facts. Link review matrix.
3. New catalog UUID/revision, publication/rollback test (or reason dev publication not done); old r2 retained.
4. Demonstrated actual API recommendation outcomes for student/creator/researcher, **separate from synthetic E2E fixtures**; include NO_MATCH if insufficient evidence.
5. UI screens and isolated E2E results, all scripts and performance regression.
6. Security checks, new direct dependencies (if none, say none), limitations and unknown prices, including KZ.
7. Modified files, Git status, exactly what remains to approve.

End with `CATALOG_EXPANSION = AWAITING_REVIEW`, and `P12–P14 = NOT_STARTED`. Stop. Do not create a commit/push without owner authorization. If one dependent task becomes blocked on data or permission, continue independent safe work and explicitly mark affected work BLOCKED instead of faking completion.
