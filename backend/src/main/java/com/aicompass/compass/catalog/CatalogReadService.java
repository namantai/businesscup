package com.aicompass.compass.catalog;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Function;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.aicompass.compass.shared.money.Money;
import static com.aicompass.compass.catalog.CatalogManifest.*;
import static com.aicompass.compass.catalog.CatalogResource.*;
import static com.aicompass.compass.catalog.CatalogViews.*;

@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class CatalogReadService {
    private final JdbcTemplate jdbc;
    private final CatalogPublicationService publication;
    private final CatalogPolicy policy;
    private final Clock clock;
    private final DefaultConversionService conversion = new DefaultConversionService();

    public CatalogReadService(JdbcTemplate jdbc, CatalogPublicationService publication, CatalogPolicy policy, Clock clock) {
        this.jdbc = jdbc; this.publication = publication; this.policy = policy; this.clock = clock;
        conversion.addConverter(Timestamp.class, Instant.class, Timestamp::toInstant);
        conversion.addConverter(OffsetDateTime.class, Instant.class, OffsetDateTime::toInstant);
    }

    public Meta meta() {
        var current = publication.current();
        if (current.versionId() == null) return new Meta(null, current.revision(), null, null, "EMPTY", List.of(), List.of(), List.of(), 0, 0, 0);
        UUID version = visible(current.versionId());
        var evidence = evidenceRows(version);
        return jdbc.queryForObject("SELECT dataset_kind, as_of FROM compass.catalog_version WHERE id = ?",
                (rs, row) -> new Meta(version, current.revision(), rs.getString(1), rs.getTimestamp(2).toInstant(), "AVAILABLE",
                        rows(REGION, version, Region.class), rows(USER_NEED, version, UserNeed.class), rows(CAPABILITY, version, Capability.class),
                        jdbc.queryForObject("SELECT count(*) FROM compass.product WHERE version_id = ?", Long.class, version),
                        evidence.stream().filter(e -> e.effectiveStatus().equals("UNKNOWN")).count(),
                        evidence.stream().filter(e -> e.effectiveStatus().equals("STALE")).count()), version);
    }

    /** Resolves the active pointer once; all normalized reads share REPEATABLE_READ. */
    public PublishedCatalogSnapshot currentSnapshot() { return snapshot(null); }

    /** Previously activated immutable versions are also used to save historical results. */
    public PublishedCatalogSnapshot snapshot(UUID requested) {
        UUID version = visible(requested);
        if (version == null) return null;
        var meta = jdbc.queryForMap("SELECT dataset_kind, as_of FROM compass.catalog_version WHERE id = ?", version);
        return new PublishedCatalogSnapshot(version, (String) meta.get("dataset_kind"),
                conversion.convert(meta.get("as_of"), Instant.class),
                rows(PRODUCT, version, Product.class), rows(SUBSCRIPTION_PLAN, version, SubscriptionPlan.class),
                rows(PRICE_OFFER, version, PriceOffer.class), rows(REGION, version, Region.class),
                rows(USER_NEED, version, UserNeed.class), rows(NEED_CAPABILITY, version, NeedCapability.class),
                rows(CAPABILITY, version, Capability.class), rows(CAPABILITY_ASSERTION, version, CapabilityAssertion.class),
                rows(PLAN_AGENT_ACCESS, version, PlanAgentAccess.class), rows(AGENT, version, Agent.class),
                rows(PLAN_MODEL_ACCESS, version, PlanModelAccess.class), rows(AI_MODEL, version, AiModel.class),
                rows(QUOTA_POOL, version, QuotaPool.class), rows(USAGE_LIMIT, version, UsageLimit.class),
                rows(ELIGIBILITY_RULE, version, EligibilityRule.class),
                jdbc.query("SELECT id, base_currency, quote_currency, rate, observed_at, evidence_id FROM compass.fx_snapshot WHERE version_id = ? ORDER BY id", mapper(FxSnapshot.class), version),
                evidenceRows(version), sourceRows(version));
    }

    private UUID visible(UUID requested) {
        UUID version = requested == null ? publication.current().versionId() : requested;
        if (version == null) return null;
        Long count = jdbc.queryForObject("""
                SELECT count(*) FROM compass.catalog_version v WHERE v.id = ?
                AND (v.dataset_kind = 'REVIEWED' OR ?) AND EXISTS
                (SELECT 1 FROM compass.catalog_publication p WHERE p.version_id = v.id)
                """, Long.class, version, policy.allowDemo());
        if (count != 1) throw missing();
        return version;
    }

    public <T> Page<T> typedPage(String path, UUID requested, String q, String kind, String productId,
            String regionId, String currency, String capabilityId, int page, int size, String sort, Class<T> type) {
        Page<?> result = page(path, requested, q, kind, productId, regionId, currency, capabilityId, page, size, sort);
        return new Page<>(result.versionId(), result.items().stream().map(type::cast).toList(), result.total(), result.page(), result.size());
    }

    public Page<Product> products(UUID requested, String q, String kind, String productId,
            String regionId, String currency, String capabilityId, String priceKind, int page, int size, String sort) {
        var result = filteredPage("products", requested, q, kind, productId, regionId, currency, capabilityId, priceKind, page, size, sort);
        return new Page<>(result.versionId(), result.items().stream().map(Product.class::cast).toList(), result.total(), page, size);
    }

    public Page<?> page(String path, UUID requested, String q, String kind, String productId,
            String regionId, String currency, String capabilityId, int page, int size, String sort) {
        return filteredPage(path, requested, q, kind, productId, regionId, currency, capabilityId, null, page, size, sort);
    }

    private Page<?> filteredPage(String path, UUID requested, String q, String kind, String productId,
            String regionId, String currency, String capabilityId, String priceKind, int page, int size, String sort) {
        CatalogResource resource = CatalogResource.from(path);
        if (page < 0 || page > 100000 || size < 1 || size > 100 || q.length() > 200) throw bad("Invalid pagination or query");
        if (!Set.of("id", "-id", "name", "-name").contains(sort)) throw bad("Unsupported sort");
        String orderColumn = sort.endsWith("name") ? "name" : "id";
        if (orderColumn.equals("name") && !Arrays.asList(resource.columns.split(", ")).contains("name")) throw bad("This resource has no name sort");
        if (!q.isEmpty() && !Arrays.asList(resource.columns.split(", ")).contains("name")) throw bad("Search is supported only for named resources");
        if (kind != null && resource != PRODUCT) throw bad("kind filter requires products");
        if (kind != null && !Set.of("APPLICATION", "AUTOMATION_TOOL").contains(kind)) throw bad("Unknown product kind");
        if (productId != null && !Set.of(PRODUCT, SUBSCRIPTION_PLAN, AGENT).contains(resource)) throw bad("productId filter is unsupported for this resource");
        if ((regionId != null || currency != null) && !Set.of(PRICE_OFFER, PRODUCT).contains(resource)) throw bad("Regional filters require offers or products");
        if (capabilityId != null && resource != PRODUCT) throw bad("Capability filter requires products");
        if (priceKind != null && !Set.of("FREE", "FIXED", "UNKNOWN").contains(priceKind)) throw bad("Unsupported price filter");
        UUID version = visible(requested);
        if (version == null) return new Page<>(null, List.of(), 0, page, size);
        StringBuilder where = new StringBuilder(" WHERE t.version_id = ?");
        List<Object> args = new ArrayList<>(List.of(version));
        if (!q.isEmpty()) {
            where.append(" AND position(lower(?) in lower(t.name)) > 0"); args.add(q);
        }
        filter(where, args, "kind", kind);
        filter(where, args, resource == PRODUCT ? "id" : "product_id", productId);
        if (resource == PRODUCT && (regionId != null || currency != null || priceKind != null)) {
            // Filter the same offer for all requested facets before counting/pagination.
            // A public list price is not a regional checkout or recurring free quota guarantee.
            where.append(" AND EXISTS (SELECT 1 FROM compass.subscription_plan sp JOIN compass.price_offer o ON o.version_id = sp.version_id AND o.plan_id = sp.id JOIN compass.evidence e ON e.version_id = o.version_id AND e.id = o.price_evidence_id WHERE sp.version_id = t.version_id AND sp.product_id = t.id");
            if (regionId != null) { where.append(" AND o.region_id = ?"); args.add(regionId); }
            if (currency != null) { where.append(" AND o.currency = ?"); args.add(currency); }
            if (priceKind != null) {
                where.append(" AND o.price_kind = ?"); args.add(priceKind);
                if (!priceKind.equals("UNKNOWN")) {
                    where.append(" AND e.status IN ('VERIFIED_SOURCE','CONDITIONAL') AND e.verified_at > ? AND o.valid_from <= ? AND (o.valid_until IS NULL OR o.valid_until > ?)");
                    args.add(Timestamp.from(clock.instant().minus(Duration.ofDays(7))));
                    args.add(Timestamp.from(clock.instant())); args.add(Timestamp.from(clock.instant()));
                }
            }
            where.append(")");
        } else if (resource == PRICE_OFFER) {
            filter(where, args, "region_id", regionId);
            filter(where, args, "currency", currency);
        }
        if (capabilityId != null) {
            // Taxonomy filter includes unknown assertions; it is not a recommendation or eligibility claim.
            where.append(" AND EXISTS (SELECT 1 FROM compass.capability_assertion a WHERE a.version_id = t.version_id AND (a.product_id = t.id OR EXISTS (SELECT 1 FROM compass.subscription_plan sp WHERE sp.version_id = t.version_id AND sp.id = a.plan_id AND sp.product_id = t.id)) AND a.capability_id = ?)");
            args.add(capabilityId);
        }
        long total = jdbc.queryForObject("SELECT count(*) FROM compass." + resource.table + " t" + where, Long.class, args.toArray());
        args.add(size); args.add((long) page * size);
        String columns = "t." + resource.columns.replace(", ", ", t.");
        String order = " ORDER BY t." + orderColumn + (sort.startsWith("-") ? " DESC" : " ASC") + ", t.id ASC LIMIT ? OFFSET ?";
        List<?> rows = jdbc.query("SELECT " + columns + " FROM compass." + resource.table + " t" + where + order, mapper(resource.type), args.toArray());
        if (resource == PRICE_OFFER) rows = rows.stream().map(r -> offer((PriceOffer) r)).toList();
        return new Page<>(version, rows, total, page, size);
    }

    private static void filter(StringBuilder where, List<Object> args, String column, String value) {
        if (value != null) { where.append(" AND t.").append(column).append(" = ?"); args.add(value); }
    }
    private <T> DataClassRowMapper<T> mapper(Class<T> type) {
        var mapper = new DataClassRowMapper<>(type);
        mapper.setConversionService(conversion);
        return mapper;
    }
    private <T> List<T> rows(CatalogResource resource, UUID version, Class<T> type) {
        return jdbc.query("SELECT " + resource.columns + " FROM compass." + resource.table + " WHERE version_id = ? ORDER BY id", mapper(type), version);
    }

    public Detail detail(String slug, UUID requested) {
        UUID version = visible(requested);
        if (version == null) throw missing();
        Product product = rows(PRODUCT, version, Product.class).stream().filter(p -> p.slug().equals(slug)).findFirst().orElseThrow(CatalogReadService::missing);
        List<SubscriptionPlan> plans = rows(SUBSCRIPTION_PLAN, version, SubscriptionPlan.class).stream().filter(p -> p.productId().equals(product.id())).toList();
        Set<String> planIds = ids(plans, SubscriptionPlan::id);
        List<PriceOffer> offers = rows(PRICE_OFFER, version, PriceOffer.class).stream().filter(o -> planIds.contains(o.planId())).toList();
        Set<String> offerIds = ids(offers, PriceOffer::id);
        var assertions = rows(CAPABILITY_ASSERTION, version, CapabilityAssertion.class).stream().filter(a -> product.id().equals(a.productId()) || planIds.contains(a.planId())).toList();
        var modelAccess = rows(PLAN_MODEL_ACCESS, version, PlanModelAccess.class).stream().filter(a -> planIds.contains(a.planId())).toList();
        var agentAccess = rows(PLAN_AGENT_ACCESS, version, PlanAgentAccess.class).stream().filter(a -> planIds.contains(a.planId())).toList();
        var models = rows(AI_MODEL, version, AiModel.class).stream().filter(m -> ids(modelAccess, PlanModelAccess::modelId).contains(m.id())).toList();
        var agents = rows(AGENT, version, Agent.class).stream().filter(a -> product.id().equals(a.productId()) || ids(agentAccess, PlanAgentAccess::agentId).contains(a.id())).toList();
        var pools = rows(QUOTA_POOL, version, QuotaPool.class).stream().filter(p -> offerIds.contains(p.offerId())).toList();
        var limits = rows(USAGE_LIMIT, version, UsageLimit.class).stream().filter(l -> offerIds.contains(l.offerId())).toList();
        var eligibility = rows(ELIGIBILITY_RULE, version, EligibilityRule.class).stream().filter(r -> offerIds.contains(r.offerId())).toList();
        Set<String> evidenceIds = new HashSet<>(List.of(product.metadataEvidenceId()));
        assertions.forEach(a -> evidenceIds.add(a.evidenceId()));
        modelAccess.forEach(a -> evidenceIds.add(a.evidenceId())); agentAccess.forEach(a -> evidenceIds.add(a.evidenceId()));
        agents.forEach(a -> evidenceIds.add(a.metadataEvidenceId())); pools.forEach(p -> evidenceIds.add(p.evidenceId()));
        eligibility.forEach(r -> evidenceIds.add(r.evidenceId()));
        offers.forEach(o -> evidenceIds.addAll(List.of(o.priceEvidenceId(), o.taxEvidenceId(), o.feeEvidenceId(), o.regionEvidenceId())));
        var evidence = evidenceRows(version).stream().filter(e -> evidenceIds.contains(e.id())).toList();
        var sources = sourceRows(version).stream().filter(s -> ids(evidence, CatalogViews.Evidence::sourceId).contains(s.id())).toList();
        return new Detail(version, product, plans, offers.stream().map(CatalogReadService::offer).toList(), assertions,
                rows(CAPABILITY, version, Capability.class), modelAccess, models, agentAccess, agents, pools, limits, eligibility,
                rows(REGION, version, Region.class), evidence, sources);
    }

    public <T> Page<T> evidencePage(UUID requested, int page, int size, boolean sources, Class<T> type) {
        if (page < 0 || page > 100000 || size < 1 || size > 100) throw bad("Invalid pagination");
        UUID version = visible(requested);
        List<?> all = version == null ? List.of() : sources ? sourceRows(version) : evidenceRows(version);
        int from = Math.min(all.size(), page * size);
        return new Page<>(version, all.subList(from, Math.min(all.size(), from + size)).stream().map(type::cast).toList(), all.size(), page, size);
    }
    private List<CatalogViews.Source> sourceRows(UUID version) {
        return jdbc.query("SELECT id, url, publisher, kind FROM compass.evidence_source WHERE version_id = ? ORDER BY id", mapper(CatalogViews.Source.class), version);
    }
    private List<CatalogViews.Evidence> evidenceRows(UUID version) {
        return jdbc.query("""
                SELECT id, source_id, claim_type, status, verified_at, observed_region_id, channel, summary, conditions
                FROM compass.evidence WHERE version_id = ? ORDER BY id
                """, (rs, row) -> {
                    Instant verified = rs.getTimestamp("verified_at") == null ? null : rs.getTimestamp("verified_at").toInstant();
                    String status = rs.getString("status"); String claim = rs.getString("claim_type");
                    return new CatalogViews.Evidence(rs.getString("id"), rs.getString("source_id"), claim, status,
                            effective(status, claim, verified), verified, rs.getString("observed_region_id"), rs.getString("channel"),
                            rs.getString("summary"), rs.getString("conditions"));
                }, version);
    }
    private String effective(String status, String claim, Instant verified) {
        long days = switch (claim) { case "PRICE", "TAX", "FEES" -> 7; case "USAGE_LIMIT", "MODEL_ACCESS", "AGENT_ACCESS" -> 14; default -> 30; };
        Duration age = claim.equals("FX") ? Duration.ofHours(24) : Duration.ofDays(days);
        if (verified != null && Set.of("VERIFIED_SOURCE", "CONDITIONAL").contains(status) && !clock.instant().isBefore(verified.plus(age))) return "STALE";
        return status;
    }
    private static Offer offer(PriceOffer value) {
        var costs = OfferCosts.from(value);
        return new Offer(value, decimal(costs.recurringMonthly()), decimal(costs.upfront()), decimal(costs.allInRecurringMonthly()), decimal(costs.allInUpfront()));
    }
    private static String decimal(Optional<Money> money) { return money.map(Money::decimalString).orElse(null); }
    private static <T> Set<String> ids(List<T> rows, Function<T, String> id) { return new HashSet<>(rows.stream().map(id).toList()); }
    private static ResponseStatusException missing() { return new ResponseStatusException(HttpStatus.NOT_FOUND, "Published catalog entry not found"); }
    private static ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
}
