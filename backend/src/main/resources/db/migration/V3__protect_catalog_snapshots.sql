-- A writer locks the version before changing a row. Publication uses the same lock,
-- preventing a concurrent insert from slipping into an already published snapshot.
CREATE FUNCTION compass.guard_catalog_row() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE
    target_id uuid;
    version_state text;
    dataset text;
BEGIN
    IF TG_OP = 'UPDATE' AND NEW.version_id <> OLD.version_id THEN
        RAISE EXCEPTION 'Moving a row between catalog versions is forbidden';
    END IF;
    target_id := CASE WHEN TG_OP = 'DELETE' THEN OLD.version_id ELSE NEW.version_id END;
    SELECT state, dataset_kind INTO version_state, dataset
        FROM compass.catalog_version WHERE id = target_id FOR UPDATE;
    IF version_state IS DISTINCT FROM 'DRAFT' THEN
        RAISE EXCEPTION 'Catalog version is missing or immutable';
    END IF;
    IF TG_TABLE_NAME = 'evidence' AND TG_OP = 'UPDATE' THEN
        RAISE EXCEPTION 'Evidence is append-only; create a new claim id';
    END IF;
    IF TG_OP <> 'DELETE' THEN
        IF TG_TABLE_NAME = 'evidence' THEN
            IF dataset = 'REVIEWED' AND NEW.status = 'DEMO' THEN
                RAISE EXCEPTION 'DEMO evidence cannot enter a REVIEWED catalog';
            END IF;
            IF NEW.verified_at > (SELECT as_of FROM compass.catalog_version WHERE id = target_id) THEN
                RAISE EXCEPTION 'Evidence verification cannot be later than catalog asOf';
            END IF;
        ELSIF TG_TABLE_NAME = 'evidence_source' THEN
            IF dataset = 'REVIEWED' AND NEW.kind = 'SYNTHETIC' THEN
                RAISE EXCEPTION 'Synthetic sources cannot enter a REVIEWED catalog';
            END IF;
        END IF;
        RETURN NEW;
    END IF;
    RETURN OLD;
END;
$$;

CREATE FUNCTION compass.guard_catalog_version() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        IF NEW.state <> 'DRAFT' THEN
            RAISE EXCEPTION 'A catalog must be inserted as a draft';
        END IF;
        RETURN NEW;
    END IF;
    IF TG_OP = 'DELETE' OR OLD.state = 'PUBLISHED' THEN
        RAISE EXCEPTION 'Catalog versions are retained and published versions are immutable';
    END IF;
    IF (to_jsonb(NEW) - 'state' - 'published_at') <> (to_jsonb(OLD) - 'state' - 'published_at') THEN
        RAISE EXCEPTION 'Catalog identity, provenance and source artifact cannot be replaced';
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER immutable_version BEFORE INSERT OR UPDATE OR DELETE ON compass.catalog_version
    FOR EACH ROW EXECUTE FUNCTION compass.guard_catalog_version();

CREATE FUNCTION compass.reject_catalog_truncate() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'Catalog snapshots must not be truncated';
END;
$$;

DO $$
DECLARE table_name text;
BEGIN
    FOREACH table_name IN ARRAY ARRAY['provider','region','evidence_source','evidence','product','model_family','ai_model','agent','capability','user_need','need_capability','exclusivity_group','subscription_plan','price_offer','plan_model_access','plan_agent_access','capability_assertion','quota_pool','usage_limit','eligibility_rule','benchmark_observation','fx_snapshot']
    LOOP
        EXECUTE format('CREATE TRIGGER immutable_catalog_row BEFORE INSERT OR UPDATE OR DELETE ON compass.%I FOR EACH ROW EXECUTE FUNCTION compass.guard_catalog_row()', table_name);
        EXECUTE format('CREATE TRIGGER no_catalog_truncate BEFORE TRUNCATE ON compass.%I FOR EACH STATEMENT EXECUTE FUNCTION compass.reject_catalog_truncate()', table_name);
    END LOOP;
END;
$$;
CREATE TRIGGER no_catalog_truncate BEFORE TRUNCATE ON compass.catalog_version
    FOR EACH STATEMENT EXECUTE FUNCTION compass.reject_catalog_truncate();

CREATE FUNCTION compass.check_catalog_claims() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE
    requirements jsonb;
    field_name text;
    expected_claim text;
    actual_claim text;
    claim_status text;
    row_data jsonb := to_jsonb(NEW);
BEGIN
    requirements := CASE TG_TABLE_NAME
        WHEN 'product' THEN '{"metadata_evidence_id":"PRODUCT_METADATA"}'::jsonb
        WHEN 'agent' THEN '{"metadata_evidence_id":"PRODUCT_METADATA"}'::jsonb
        WHEN 'price_offer' THEN '{"price_evidence_id":"PRICE","tax_evidence_id":"TAX","fee_evidence_id":"FEES","region_evidence_id":"REGION"}'::jsonb
        WHEN 'plan_model_access' THEN '{"evidence_id":"MODEL_ACCESS"}'::jsonb
        WHEN 'plan_agent_access' THEN '{"evidence_id":"AGENT_ACCESS"}'::jsonb
        WHEN 'capability_assertion' THEN '{"evidence_id":"CAPABILITY"}'::jsonb
        WHEN 'quota_pool' THEN '{"evidence_id":"USAGE_LIMIT"}'::jsonb
        WHEN 'eligibility_rule' THEN '{"evidence_id":"ELIGIBILITY"}'::jsonb
        WHEN 'benchmark_observation' THEN '{"evidence_id":"BENCHMARK"}'::jsonb
        WHEN 'fx_snapshot' THEN '{"evidence_id":"FX"}'::jsonb
        ELSE '{}'::jsonb END;
    FOR field_name, expected_claim IN SELECT * FROM jsonb_each_text(requirements) LOOP
        SELECT claim_type, status INTO actual_claim, claim_status FROM compass.evidence
            WHERE version_id = NEW.version_id AND id = row_data ->> field_name;
        IF actual_claim IS DISTINCT FROM expected_claim THEN
            RAISE EXCEPTION 'Evidence must support field %', field_name;
        END IF;
        IF claim_status = 'UNKNOWN' THEN
            IF (field_name = 'price_evidence_id' AND row_data ->> 'price_kind' <> 'UNKNOWN')
                OR (field_name = 'tax_evidence_id' AND row_data ->> 'tax_status' <> 'UNKNOWN')
                OR (field_name = 'fee_evidence_id' AND row_data ->> 'fee_status' <> 'UNKNOWN')
                OR (field_name = 'evidence_id' AND TG_TABLE_NAME IN
                    ('capability_assertion','plan_model_access','plan_agent_access','quota_pool','eligibility_rule')
                    AND row_data ->> 'state' <> 'UNKNOWN')
                OR (field_name = 'evidence_id' AND TG_TABLE_NAME IN ('fx_snapshot','benchmark_observation')) THEN
                RAISE EXCEPTION 'UNKNOWN evidence cannot establish a known value';
            END IF;
        END IF;
    END LOOP;
    RETURN NEW;
END;
$$;
DO $$
DECLARE table_name text;
BEGIN
    FOREACH table_name IN ARRAY ARRAY['product','agent','price_offer','plan_model_access','plan_agent_access',
        'capability_assertion','quota_pool','eligibility_rule','benchmark_observation','fx_snapshot'] LOOP
        EXECUTE format('CREATE TRIGGER check_claim_fields BEFORE INSERT OR UPDATE ON compass.%I FOR EACH ROW EXECUTE FUNCTION compass.check_catalog_claims()', table_name);
    END LOOP;
END;
$$;
