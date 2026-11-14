-- Version-scoped composite foreign keys prevent mixing catalog snapshots.
CREATE DOMAIN compass.money_amount AS numeric
    CHECK (VALUE >= 0 AND VALUE < 1000000000000 AND scale(VALUE) <= 6);
CREATE DOMAIN compass.quantity_amount AS numeric
    CHECK (VALUE >= 0 AND VALUE < 1000000000000 AND scale(VALUE) <= 6);
CREATE DOMAIN compass.measurement_value AS numeric
    CHECK (VALUE > -1000000000000 AND VALUE < 1000000000000 AND scale(VALUE) <= 6);
CREATE DOMAIN compass.fx_rate AS numeric
    CHECK (VALUE > 0 AND VALUE < 1000000000000 AND scale(VALUE) <= 12);
-- ISO monetary codes from the pinned JDK; includes historical codes for snapshots.
CREATE DOMAIN compass.currency_code AS text CHECK (VALUE IN (
    'ADP','AED','AFA','AFN','ALL','AMD','ANG','AOA','ARS','ATS','AUD','AWG','AYM','AZM','AZN','BAM','BBD','BDT','BEF','BGL','BGN','BHD','BIF','BMD','BND','BOB','BOV','BRL','BSD','BTN','BWP','BYB','BYN','BYR','BZD','CAD','CDF','CHE','CHF','CHW','CLF','CLP','CNY','COP','COU','CRC','CSD','CUC','CUP','CVE','CYP','CZK','DEM','DJF','DKK','DOP','DZD','EEK','EGP','ERN','ESP','ETB','EUR','FIM','FJD','FKP','FRF','GBP','GEL','GHC','GHS','GIP','GMD','GNF','GRD','GTQ','GWP','GYD','HKD','HNL','HRK','HTG','HUF','IDR','IEP','ILS','INR','IQD','IRR','ISK','ITL','JMD','JOD','JPY','KES','KGS','KHR','KMF','KPW','KRW','KWD','KYD','KZT','LAK','LBP','LKR','LRD','LSL','LTL','LUF','LVL','LYD','MAD','MDL','MGA','MGF','MKD','MMK','MNT','MOP','MRO','MRU','MTL','MUR','MVR','MWK','MXN','MXV','MYR','MZM','MZN','NAD','NGN','NIO','NLG','NOK','NPR','NZD','OMR','PAB','PEN','PGK','PHP','PKR','PLN','PTE','PYG','QAR','ROL','RON','RSD','RUB','RUR','RWF','SAR','SBD','SCR','SDD','SDG','SEK','SGD','SHP','SIT','SKK','SLE','SLL','SOS','SRD','SRG','SSP','STD','STN','SVC','SYP','SZL','THB','TJS','TMM','TMT','TND','TOP','TPE','TRL','TRY','TTD','TWD','TZS','UAH','UGX','USD','USN','USS','UYI','UYU','UZS','VEB','VED','VEF','VES','VND','VUV','WST','XAD','XAF','XCD','XCG','XOF','XPF','YER','YUM','ZAR','ZMK','ZMW','ZWD','ZWG','ZWL','ZWN','ZWR'
));

CREATE TABLE compass.catalog_version (
    id uuid PRIMARY KEY,
    schema_version integer NOT NULL CHECK (schema_version = 1),
    dataset_kind text NOT NULL CHECK (dataset_kind IN ('DEMO','REVIEWED')),
    state text NOT NULL CHECK (state IN ('DRAFT','PUBLISHED')),
    content_hash text NOT NULL UNIQUE CHECK (content_hash ~ '^[a-f0-9]{64}$'),
    as_of timestamptz NOT NULL,
    published_at timestamptz,
    reviewer text NOT NULL CHECK (length(trim(reviewer)) > 0),
    manifest_json text NOT NULL,
    CHECK ((state = 'DRAFT' AND published_at IS NULL) OR
           (state = 'PUBLISHED' AND published_at IS NOT NULL AND published_at >= as_of))
);

CREATE TABLE compass.provider (
    version_id uuid NOT NULL REFERENCES compass.catalog_version(id),
    id text NOT NULL CHECK (id ~ '^[a-z][a-z0-9-]{0,79}$'),
    name text NOT NULL CHECK (length(trim(name)) BETWEEN 1 AND 2000),
    official_domain text NOT NULL CHECK (length(trim(official_domain)) BETWEEN 1 AND 2000),
    PRIMARY KEY (version_id, id),
    UNIQUE NULLS NOT DISTINCT (version_id, official_domain)
);


CREATE TABLE compass.region (
    version_id uuid NOT NULL REFERENCES compass.catalog_version(id),
    id text NOT NULL CHECK (id ~ '^[a-z][a-z0-9-]{0,79}$'),
    country_code text NOT NULL CHECK (country_code ~ '^[A-Z]{2}$'),
    name text NOT NULL CHECK (length(trim(name)) BETWEEN 1 AND 2000),
    PRIMARY KEY (version_id, id),
    UNIQUE NULLS NOT DISTINCT (version_id, country_code)
);


CREATE TABLE compass.evidence_source (
    version_id uuid NOT NULL REFERENCES compass.catalog_version(id),
    id text NOT NULL CHECK (id ~ '^[a-z][a-z0-9-]{0,79}$'),
    url text NOT NULL CHECK (length(trim(url)) BETWEEN 1 AND 2000 AND url ~ '^https://'),
    publisher text NOT NULL CHECK (length(trim(publisher)) BETWEEN 1 AND 2000),
    kind text NOT NULL CHECK (kind IN ('OFFICIAL','INDEPENDENT_BENCHMARK','USER_REPORT','SYNTHETIC')),
    usage_rights text NOT NULL CHECK (length(trim(usage_rights)) BETWEEN 1 AND 2000),
    PRIMARY KEY (version_id, id),
    UNIQUE NULLS NOT DISTINCT (version_id, url)
);


CREATE TABLE compass.evidence (
    version_id uuid NOT NULL REFERENCES compass.catalog_version(id),
    id text NOT NULL CHECK (id ~ '^[a-z][a-z0-9-]{0,79}$'),
    source_id text CHECK (source_id ~ '^[a-z][a-z0-9-]{0,79}$'),
    claim_type text NOT NULL CHECK (claim_type IN ('PRICE','TAX','FEES','REGION','MODEL_ACCESS','AGENT_ACCESS','CAPABILITY','USAGE_LIMIT','ELIGIBILITY','BENCHMARK','FX','PRODUCT_METADATA')),
    status text NOT NULL CHECK (status IN ('VERIFIED_SOURCE','CONDITIONAL','UNKNOWN','STALE','CONFLICTED','DEMO')),
    verified_at timestamptz,
    observed_region_id text CHECK (observed_region_id ~ '^[a-z][a-z0-9-]{0,79}$'),
    channel text NOT NULL CHECK (length(trim(channel)) BETWEEN 1 AND 2000),
    summary text NOT NULL CHECK (length(trim(summary)) BETWEEN 1 AND 2000),
    reviewer text NOT NULL CHECK (length(trim(reviewer)) BETWEEN 1 AND 2000),
    method text NOT NULL CHECK (length(trim(method)) BETWEEN 1 AND 2000),
    conditions text NOT NULL CHECK (length(trim(conditions)) BETWEEN 1 AND 2000),
    PRIMARY KEY (version_id, id),
    FOREIGN KEY (version_id, source_id) REFERENCES compass.evidence_source(version_id, id),
    FOREIGN KEY (version_id, observed_region_id) REFERENCES compass.region(version_id, id),
    CHECK (((status = 'UNKNOWN' OR (source_id IS NOT NULL AND verified_at IS NOT NULL))) IS TRUE)
);

CREATE INDEX ON compass.evidence (version_id, source_id);
CREATE INDEX ON compass.evidence (version_id, observed_region_id);

CREATE TABLE compass.product (
    version_id uuid NOT NULL REFERENCES compass.catalog_version(id),
    id text NOT NULL CHECK (id ~ '^[a-z][a-z0-9-]{0,79}$'),
    provider_id text NOT NULL CHECK (provider_id ~ '^[a-z][a-z0-9-]{0,79}$'),
    slug text NOT NULL CHECK (slug ~ '^[a-z][a-z0-9-]{0,79}$'),
    name text NOT NULL CHECK (length(trim(name)) BETWEEN 1 AND 2000),
    kind text NOT NULL CHECK (kind IN ('APPLICATION','AUTOMATION_TOOL')),
    state text NOT NULL CHECK (state IN ('ACTIVE','RETIRED')),
    metadata_evidence_id text NOT NULL CHECK (metadata_evidence_id ~ '^[a-z][a-z0-9-]{0,79}$'),
    PRIMARY KEY (version_id, id),
    FOREIGN KEY (version_id, provider_id) REFERENCES compass.provider(version_id, id),
    FOREIGN KEY (version_id, metadata_evidence_id) REFERENCES compass.evidence(version_id, id),
    UNIQUE NULLS NOT DISTINCT (version_id, slug)
);

CREATE INDEX ON compass.product (version_id, provider_id);
CREATE INDEX ON compass.product (version_id, metadata_evidence_id);

CREATE TABLE compass.model_family (
    version_id uuid NOT NULL REFERENCES compass.catalog_version(id),
    id text NOT NULL CHECK (id ~ '^[a-z][a-z0-9-]{0,79}$'),
    provider_id text NOT NULL CHECK (provider_id ~ '^[a-z][a-z0-9-]{0,79}$'),
    name text NOT NULL CHECK (length(trim(name)) BETWEEN 1 AND 2000),
    PRIMARY KEY (version_id, id),
    FOREIGN KEY (version_id, provider_id) REFERENCES compass.provider(version_id, id),
    UNIQUE NULLS NOT DISTINCT (version_id, provider_id, name)
);

CREATE INDEX ON compass.model_family (version_id, provider_id);

CREATE TABLE compass.ai_model (
    version_id uuid NOT NULL REFERENCES compass.catalog_version(id),
    id text NOT NULL CHECK (id ~ '^[a-z][a-z0-9-]{0,79}$'),
    family_id text NOT NULL CHECK (family_id ~ '^[a-z][a-z0-9-]{0,79}$'),
    name text NOT NULL CHECK (length(trim(name)) BETWEEN 1 AND 2000),
    exact_version text NOT NULL CHECK (length(trim(exact_version)) BETWEEN 1 AND 2000),
    PRIMARY KEY (version_id, id),
    FOREIGN KEY (version_id, family_id) REFERENCES compass.model_family(version_id, id),
    UNIQUE NULLS NOT DISTINCT (version_id, family_id, exact_version)
);

CREATE INDEX ON compass.ai_model (version_id, family_id);

CREATE TABLE compass.agent (
    version_id uuid NOT NULL REFERENCES compass.catalog_version(id),
    id text NOT NULL CHECK (id ~ '^[a-z][a-z0-9-]{0,79}$'),
    provider_id text NOT NULL CHECK (provider_id ~ '^[a-z][a-z0-9-]{0,79}$'),
    product_id text CHECK (product_id ~ '^[a-z][a-z0-9-]{0,79}$'),
    name text NOT NULL CHECK (length(trim(name)) BETWEEN 1 AND 2000),
    execution_environment text NOT NULL CHECK (length(trim(execution_environment)) BETWEEN 1 AND 2000),
    metadata_evidence_id text NOT NULL CHECK (metadata_evidence_id ~ '^[a-z][a-z0-9-]{0,79}$'),
    PRIMARY KEY (version_id, id),
    FOREIGN KEY (version_id, provider_id) REFERENCES compass.provider(version_id, id),
    FOREIGN KEY (version_id, product_id) REFERENCES compass.product(version_id, id),
    FOREIGN KEY (version_id, metadata_evidence_id) REFERENCES compass.evidence(version_id, id)
);

CREATE INDEX ON compass.agent (version_id, provider_id);
CREATE INDEX ON compass.agent (version_id, product_id);
CREATE INDEX ON compass.agent (version_id, metadata_evidence_id);

CREATE TABLE compass.capability (
    version_id uuid NOT NULL REFERENCES compass.catalog_version(id),
    id text NOT NULL CHECK (id ~ '^[a-z][a-z0-9-]{0,79}$'),
    category text NOT NULL CHECK (category IN ('FUNCTION','PLATFORM','INTERFACE_LANGUAGE','OUTPUT_LANGUAGE','PRIVACY','LICENSE')),
    name text NOT NULL CHECK (length(trim(name)) BETWEEN 1 AND 2000),
    PRIMARY KEY (version_id, id)
);


CREATE TABLE compass.user_need (
    version_id uuid NOT NULL REFERENCES compass.catalog_version(id),
    id text NOT NULL CHECK (id ~ '^[a-z][a-z0-9-]{0,79}$'),
    name text NOT NULL CHECK (length(trim(name)) BETWEEN 1 AND 2000),
    PRIMARY KEY (version_id, id)
);


CREATE TABLE compass.need_capability (
    version_id uuid NOT NULL REFERENCES compass.catalog_version(id),
    id text NOT NULL CHECK (id ~ '^[a-z][a-z0-9-]{0,79}$'),
    need_id text NOT NULL CHECK (need_id ~ '^[a-z][a-z0-9-]{0,79}$'),
    capability_id text NOT NULL CHECK (capability_id ~ '^[a-z][a-z0-9-]{0,79}$'),
    PRIMARY KEY (version_id, id),
    FOREIGN KEY (version_id, need_id) REFERENCES compass.user_need(version_id, id),
    FOREIGN KEY (version_id, capability_id) REFERENCES compass.capability(version_id, id),
    UNIQUE NULLS NOT DISTINCT (version_id, need_id, capability_id)
);

CREATE INDEX ON compass.need_capability (version_id, need_id);
CREATE INDEX ON compass.need_capability (version_id, capability_id);

CREATE TABLE compass.exclusivity_group (
    version_id uuid NOT NULL REFERENCES compass.catalog_version(id),
    id text NOT NULL CHECK (id ~ '^[a-z][a-z0-9-]{0,79}$'),
    product_id text NOT NULL CHECK (product_id ~ '^[a-z][a-z0-9-]{0,79}$'),
    name text NOT NULL CHECK (length(trim(name)) BETWEEN 1 AND 2000),
    PRIMARY KEY (version_id, id),
    FOREIGN KEY (version_id, product_id) REFERENCES compass.product(version_id, id),
    UNIQUE NULLS NOT DISTINCT (version_id, id, product_id),
    UNIQUE NULLS NOT DISTINCT (version_id, product_id, name)
);

CREATE INDEX ON compass.exclusivity_group (version_id, product_id);

CREATE TABLE compass.subscription_plan (
    version_id uuid NOT NULL REFERENCES compass.catalog_version(id),
    id text NOT NULL CHECK (id ~ '^[a-z][a-z0-9-]{0,79}$'),
    product_id text NOT NULL CHECK (product_id ~ '^[a-z][a-z0-9-]{0,79}$'),
    tier text NOT NULL CHECK (length(trim(tier)) BETWEEN 1 AND 2000),
    access_type text NOT NULL CHECK (access_type IN ('APPLICATION','API','BOTH')),
    exclusivity_group_id text CHECK (exclusivity_group_id ~ '^[a-z][a-z0-9-]{0,79}$'),
    active_from timestamptz NOT NULL,
    active_until timestamptz,
    PRIMARY KEY (version_id, id),
    FOREIGN KEY (version_id, product_id) REFERENCES compass.product(version_id, id),
    CHECK ((active_until IS NULL OR active_until > active_from) IS TRUE),
    UNIQUE NULLS NOT DISTINCT (version_id, product_id, tier),
    FOREIGN KEY (version_id, exclusivity_group_id, product_id) REFERENCES compass.exclusivity_group(version_id, id, product_id)
);

CREATE INDEX ON compass.subscription_plan (version_id, product_id);

CREATE TABLE compass.price_offer (
    version_id uuid NOT NULL REFERENCES compass.catalog_version(id),
    id text NOT NULL CHECK (id ~ '^[a-z][a-z0-9-]{0,79}$'),
    plan_id text NOT NULL CHECK (plan_id ~ '^[a-z][a-z0-9-]{0,79}$'),
    region_id text NOT NULL CHECK (region_id ~ '^[a-z][a-z0-9-]{0,79}$'),
    channel text NOT NULL CHECK (length(trim(channel)) BETWEEN 1 AND 2000),
    currency compass.currency_code NOT NULL,
    price_kind text NOT NULL CHECK (price_kind IN ('FREE','FIXED','UNKNOWN','TRIAL','USAGE_BASED')),
    billing_period text NOT NULL CHECK (billing_period IN ('MONTH','YEAR','ONE_TIME','USAGE')),
    amount compass.money_amount,
    upfront_amount compass.money_amount,
    usage_unit text CHECK (length(trim(usage_unit)) BETWEEN 1 AND 2000),
    tax_status text NOT NULL CHECK (tax_status IN ('INCLUDED','EXCLUDED_KNOWN','NOT_APPLICABLE','UNKNOWN')),
    additional_tax compass.money_amount,
    fee_status text NOT NULL CHECK (fee_status IN ('KNOWN','UNKNOWN')),
    additional_fee compass.money_amount,
    trial_days integer CHECK (trial_days BETWEEN 1 AND 3660),
    conditions text NOT NULL CHECK (length(trim(conditions)) BETWEEN 1 AND 2000),
    valid_from timestamptz NOT NULL,
    valid_until timestamptz,
    price_evidence_id text NOT NULL CHECK (price_evidence_id ~ '^[a-z][a-z0-9-]{0,79}$'),
    tax_evidence_id text NOT NULL CHECK (tax_evidence_id ~ '^[a-z][a-z0-9-]{0,79}$'),
    fee_evidence_id text NOT NULL CHECK (fee_evidence_id ~ '^[a-z][a-z0-9-]{0,79}$'),
    region_evidence_id text NOT NULL CHECK (region_evidence_id ~ '^[a-z][a-z0-9-]{0,79}$'),
    PRIMARY KEY (version_id, id),
    FOREIGN KEY (version_id, plan_id) REFERENCES compass.subscription_plan(version_id, id),
    FOREIGN KEY (version_id, region_id) REFERENCES compass.region(version_id, id),
    FOREIGN KEY (version_id, price_evidence_id) REFERENCES compass.evidence(version_id, id),
    FOREIGN KEY (version_id, tax_evidence_id) REFERENCES compass.evidence(version_id, id),
    FOREIGN KEY (version_id, fee_evidence_id) REFERENCES compass.evidence(version_id, id),
    FOREIGN KEY (version_id, region_evidence_id) REFERENCES compass.evidence(version_id, id),
    CHECK (((price_kind = 'UNKNOWN' AND amount IS NULL AND upfront_amount IS NULL) OR (price_kind = 'FREE' AND amount = 0 AND upfront_amount = 0 AND billing_period = 'MONTH') OR (price_kind = 'FIXED' AND amount > 0 AND upfront_amount >= amount AND billing_period IN ('MONTH','YEAR','ONE_TIME')) OR (price_kind = 'TRIAL' AND amount IS NOT NULL AND upfront_amount >= amount AND billing_period = 'ONE_TIME') OR (price_kind = 'USAGE_BASED' AND amount > 0 AND billing_period = 'USAGE' AND upfront_amount IS NULL)) IS TRUE),
    CHECK (((price_kind = 'TRIAL') = (trial_days IS NOT NULL)) IS TRUE),
    CHECK (((price_kind = 'USAGE_BASED') = (usage_unit IS NOT NULL)) IS TRUE),
    CHECK (((tax_status = 'UNKNOWN' AND additional_tax IS NULL) OR (tax_status IN ('INCLUDED','NOT_APPLICABLE') AND additional_tax = 0) OR (tax_status = 'EXCLUDED_KNOWN' AND additional_tax IS NOT NULL)) IS TRUE),
    CHECK (((fee_status = 'UNKNOWN' AND additional_fee IS NULL) OR (fee_status = 'KNOWN' AND additional_fee IS NOT NULL)) IS TRUE),
    CHECK ((valid_until IS NULL OR valid_until > valid_from) IS TRUE),
    UNIQUE NULLS NOT DISTINCT (version_id, plan_id, region_id, channel, billing_period, valid_from)
);

CREATE INDEX ON compass.price_offer (version_id, plan_id);
CREATE INDEX ON compass.price_offer (version_id, region_id);
CREATE INDEX ON compass.price_offer (version_id, price_evidence_id);
CREATE INDEX ON compass.price_offer (version_id, tax_evidence_id);
CREATE INDEX ON compass.price_offer (version_id, fee_evidence_id);
CREATE INDEX ON compass.price_offer (version_id, region_evidence_id);

CREATE TABLE compass.plan_model_access (
    version_id uuid NOT NULL REFERENCES compass.catalog_version(id),
    id text NOT NULL CHECK (id ~ '^[a-z][a-z0-9-]{0,79}$'),
    plan_id text NOT NULL CHECK (plan_id ~ '^[a-z][a-z0-9-]{0,79}$'),
    model_id text NOT NULL CHECK (model_id ~ '^[a-z][a-z0-9-]{0,79}$'),
    mode text NOT NULL CHECK (length(trim(mode)) BETWEEN 1 AND 2000),
    state text NOT NULL CHECK (state IN ('AVAILABLE','CONDITIONAL','UNKNOWN')),
    conditions text NOT NULL CHECK (length(trim(conditions)) BETWEEN 1 AND 2000),
    evidence_id text NOT NULL CHECK (evidence_id ~ '^[a-z][a-z0-9-]{0,79}$'),
    PRIMARY KEY (version_id, id),
    FOREIGN KEY (version_id, plan_id) REFERENCES compass.subscription_plan(version_id, id),
    FOREIGN KEY (version_id, model_id) REFERENCES compass.ai_model(version_id, id),
    FOREIGN KEY (version_id, evidence_id) REFERENCES compass.evidence(version_id, id),
    UNIQUE NULLS NOT DISTINCT (version_id, plan_id, model_id, mode)
);

CREATE INDEX ON compass.plan_model_access (version_id, plan_id);
CREATE INDEX ON compass.plan_model_access (version_id, model_id);
CREATE INDEX ON compass.plan_model_access (version_id, evidence_id);

CREATE TABLE compass.plan_agent_access (
    version_id uuid NOT NULL REFERENCES compass.catalog_version(id),
    id text NOT NULL CHECK (id ~ '^[a-z][a-z0-9-]{0,79}$'),
    plan_id text NOT NULL CHECK (plan_id ~ '^[a-z][a-z0-9-]{0,79}$'),
    agent_id text NOT NULL CHECK (agent_id ~ '^[a-z][a-z0-9-]{0,79}$'),
    state text NOT NULL CHECK (state IN ('AVAILABLE','CONDITIONAL','UNKNOWN')),
    conditions text NOT NULL CHECK (length(trim(conditions)) BETWEEN 1 AND 2000),
    evidence_id text NOT NULL CHECK (evidence_id ~ '^[a-z][a-z0-9-]{0,79}$'),
    PRIMARY KEY (version_id, id),
    FOREIGN KEY (version_id, plan_id) REFERENCES compass.subscription_plan(version_id, id),
    FOREIGN KEY (version_id, agent_id) REFERENCES compass.agent(version_id, id),
    FOREIGN KEY (version_id, evidence_id) REFERENCES compass.evidence(version_id, id),
    UNIQUE NULLS NOT DISTINCT (version_id, plan_id, agent_id)
);

CREATE INDEX ON compass.plan_agent_access (version_id, plan_id);
CREATE INDEX ON compass.plan_agent_access (version_id, agent_id);
CREATE INDEX ON compass.plan_agent_access (version_id, evidence_id);

CREATE TABLE compass.capability_assertion (
    version_id uuid NOT NULL REFERENCES compass.catalog_version(id),
    id text NOT NULL CHECK (id ~ '^[a-z][a-z0-9-]{0,79}$'),
    product_id text CHECK (product_id ~ '^[a-z][a-z0-9-]{0,79}$'),
    plan_id text CHECK (plan_id ~ '^[a-z][a-z0-9-]{0,79}$'),
    capability_id text NOT NULL CHECK (capability_id ~ '^[a-z][a-z0-9-]{0,79}$'),
    state text NOT NULL CHECK (state IN ('SUPPORTED','PARTIAL','UNSUPPORTED','UNKNOWN')),
    conditions text NOT NULL CHECK (length(trim(conditions)) BETWEEN 1 AND 2000),
    evidence_id text NOT NULL CHECK (evidence_id ~ '^[a-z][a-z0-9-]{0,79}$'),
    PRIMARY KEY (version_id, id),
    FOREIGN KEY (version_id, product_id) REFERENCES compass.product(version_id, id),
    FOREIGN KEY (version_id, plan_id) REFERENCES compass.subscription_plan(version_id, id),
    FOREIGN KEY (version_id, capability_id) REFERENCES compass.capability(version_id, id),
    FOREIGN KEY (version_id, evidence_id) REFERENCES compass.evidence(version_id, id),
    CHECK ((num_nonnulls(product_id, plan_id) = 1) IS TRUE),
    UNIQUE NULLS NOT DISTINCT (version_id, product_id, plan_id, capability_id)
);

CREATE INDEX ON compass.capability_assertion (version_id, product_id);
CREATE INDEX ON compass.capability_assertion (version_id, plan_id);
CREATE INDEX ON compass.capability_assertion (version_id, capability_id);
CREATE INDEX ON compass.capability_assertion (version_id, evidence_id);

CREATE TABLE compass.quota_pool (
    version_id uuid NOT NULL REFERENCES compass.catalog_version(id),
    id text NOT NULL CHECK (id ~ '^[a-z][a-z0-9-]{0,79}$'),
    offer_id text NOT NULL CHECK (offer_id ~ '^[a-z][a-z0-9-]{0,79}$'),
    state text NOT NULL CHECK (state IN ('KNOWN','UNKNOWN','UNLIMITED')),
    quantity compass.quantity_amount,
    unit text NOT NULL CHECK (length(trim(unit)) BETWEEN 1 AND 2000),
    reset_period text NOT NULL CHECK (reset_period IN ('DAY','MONTH','YEAR','NEVER','UNKNOWN')),
    expires_at timestamptz,
    conditions text NOT NULL CHECK (length(trim(conditions)) BETWEEN 1 AND 2000),
    evidence_id text NOT NULL CHECK (evidence_id ~ '^[a-z][a-z0-9-]{0,79}$'),
    PRIMARY KEY (version_id, id),
    FOREIGN KEY (version_id, offer_id) REFERENCES compass.price_offer(version_id, id),
    FOREIGN KEY (version_id, evidence_id) REFERENCES compass.evidence(version_id, id),
    CHECK (((state = 'KNOWN' AND quantity IS NOT NULL) OR (state IN ('UNKNOWN','UNLIMITED') AND quantity IS NULL)) IS TRUE),
    CHECK ((reset_period <> 'NEVER' OR expires_at IS NOT NULL) IS TRUE),
    UNIQUE NULLS NOT DISTINCT (version_id, id, offer_id)
);

CREATE INDEX ON compass.quota_pool (version_id, offer_id);
CREATE INDEX ON compass.quota_pool (version_id, evidence_id);

CREATE TABLE compass.usage_limit (
    version_id uuid NOT NULL REFERENCES compass.catalog_version(id),
    id text NOT NULL CHECK (id ~ '^[a-z][a-z0-9-]{0,79}$'),
    offer_id text NOT NULL CHECK (offer_id ~ '^[a-z][a-z0-9-]{0,79}$'),
    capability_id text NOT NULL CHECK (capability_id ~ '^[a-z][a-z0-9-]{0,79}$'),
    pool_id text NOT NULL CHECK (pool_id ~ '^[a-z][a-z0-9-]{0,79}$'),
    mode text NOT NULL CHECK (length(trim(mode)) BETWEEN 1 AND 2000),
    units_per_use compass.quantity_amount,
    PRIMARY KEY (version_id, id),
    FOREIGN KEY (version_id, offer_id) REFERENCES compass.price_offer(version_id, id),
    FOREIGN KEY (version_id, capability_id) REFERENCES compass.capability(version_id, id),
    CHECK ((units_per_use IS NULL OR units_per_use > 0) IS TRUE),
    UNIQUE NULLS NOT DISTINCT (version_id, offer_id, capability_id, mode),
    FOREIGN KEY (version_id, pool_id, offer_id) REFERENCES compass.quota_pool(version_id, id, offer_id)
);

CREATE INDEX ON compass.usage_limit (version_id, offer_id);
CREATE INDEX ON compass.usage_limit (version_id, capability_id);

CREATE TABLE compass.eligibility_rule (
    version_id uuid NOT NULL REFERENCES compass.catalog_version(id),
    id text NOT NULL CHECK (id ~ '^[a-z][a-z0-9-]{0,79}$'),
    offer_id text NOT NULL CHECK (offer_id ~ '^[a-z][a-z0-9-]{0,79}$'),
    kind text NOT NULL CHECK (kind IN ('STUDENT','REGION','PLATFORM','BILLING','PRIVACY','LANGUAGE')),
    state text NOT NULL CHECK (state IN ('REQUIRED','NOT_REQUIRED','UNKNOWN')),
    requirement text NOT NULL CHECK (length(trim(requirement)) BETWEEN 1 AND 2000),
    conditions text NOT NULL CHECK (length(trim(conditions)) BETWEEN 1 AND 2000),
    evidence_id text NOT NULL CHECK (evidence_id ~ '^[a-z][a-z0-9-]{0,79}$'),
    PRIMARY KEY (version_id, id),
    FOREIGN KEY (version_id, offer_id) REFERENCES compass.price_offer(version_id, id),
    FOREIGN KEY (version_id, evidence_id) REFERENCES compass.evidence(version_id, id),
    UNIQUE NULLS NOT DISTINCT (version_id, offer_id, kind, requirement)
);

CREATE INDEX ON compass.eligibility_rule (version_id, offer_id);
CREATE INDEX ON compass.eligibility_rule (version_id, evidence_id);

CREATE TABLE compass.benchmark_observation (
    version_id uuid NOT NULL REFERENCES compass.catalog_version(id),
    id text NOT NULL CHECK (id ~ '^[a-z][a-z0-9-]{0,79}$'),
    model_id text NOT NULL CHECK (model_id ~ '^[a-z][a-z0-9-]{0,79}$'),
    task text NOT NULL CHECK (length(trim(task)) BETWEEN 1 AND 2000),
    metric text NOT NULL CHECK (length(trim(metric)) BETWEEN 1 AND 2000),
    value compass.measurement_value NOT NULL,
    unit text NOT NULL CHECK (length(trim(unit)) BETWEEN 1 AND 2000),
    methodology text NOT NULL CHECK (length(trim(methodology)) BETWEEN 1 AND 2000),
    observed_at timestamptz NOT NULL,
    evidence_id text NOT NULL CHECK (evidence_id ~ '^[a-z][a-z0-9-]{0,79}$'),
    PRIMARY KEY (version_id, id),
    FOREIGN KEY (version_id, model_id) REFERENCES compass.ai_model(version_id, id),
    FOREIGN KEY (version_id, evidence_id) REFERENCES compass.evidence(version_id, id),
    UNIQUE NULLS NOT DISTINCT (version_id, model_id, task, metric, methodology, observed_at)
);

CREATE INDEX ON compass.benchmark_observation (version_id, model_id);
CREATE INDEX ON compass.benchmark_observation (version_id, evidence_id);

CREATE TABLE compass.fx_snapshot (
    version_id uuid NOT NULL REFERENCES compass.catalog_version(id),
    id text NOT NULL CHECK (id ~ '^[a-z][a-z0-9-]{0,79}$'),
    base_currency compass.currency_code NOT NULL,
    quote_currency compass.currency_code NOT NULL,
    rate compass.fx_rate NOT NULL,
    observed_at timestamptz NOT NULL,
    evidence_id text NOT NULL CHECK (evidence_id ~ '^[a-z][a-z0-9-]{0,79}$'),
    PRIMARY KEY (version_id, id),
    FOREIGN KEY (version_id, evidence_id) REFERENCES compass.evidence(version_id, id),
    CHECK ((base_currency <> quote_currency) IS TRUE),
    UNIQUE NULLS NOT DISTINCT (version_id, base_currency, quote_currency, observed_at)
);

CREATE INDEX ON compass.fx_snapshot (version_id, evidence_id);
