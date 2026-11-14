package com.aicompass.compass.catalog;

public final class CatalogTypes {
    private CatalogTypes() {}
    public enum DatasetKind { DEMO, REVIEWED }
    public enum ProductKind { APPLICATION, AUTOMATION_TOOL }
    public enum ProductState { ACTIVE, RETIRED }
    public enum AccessType { APPLICATION, API, BOTH }
    public enum EvidenceStatus { VERIFIED_SOURCE, CONDITIONAL, UNKNOWN, STALE, CONFLICTED, DEMO }
    public enum SourceKind { OFFICIAL, INDEPENDENT_BENCHMARK, USER_REPORT, SYNTHETIC }
    public enum ClaimType { PRICE, TAX, FEES, REGION, MODEL_ACCESS, AGENT_ACCESS, CAPABILITY, USAGE_LIMIT, ELIGIBILITY, BENCHMARK, FX, PRODUCT_METADATA }
    public enum CapabilityCategory { FUNCTION, PLATFORM, INTERFACE_LANGUAGE, OUTPUT_LANGUAGE, PRIVACY, LICENSE }
    public enum CapabilityState { SUPPORTED, PARTIAL, UNSUPPORTED, UNKNOWN }
    public enum BillingPeriod { MONTH, YEAR, ONE_TIME, USAGE }
    public enum PriceKind { FREE, FIXED, UNKNOWN, TRIAL, USAGE_BASED }
    public enum TaxStatus { INCLUDED, EXCLUDED_KNOWN, NOT_APPLICABLE, UNKNOWN }
    public enum ValueStatus { KNOWN, UNKNOWN }
    public enum LimitState { KNOWN, UNKNOWN, UNLIMITED }
    public enum ResetPeriod { DAY, MONTH, YEAR, NEVER, UNKNOWN }
    public enum EligibilityKind { STUDENT, REGION, PLATFORM, BILLING, PRIVACY, LANGUAGE }
    public enum EligibilityState { REQUIRED, NOT_REQUIRED, UNKNOWN }
    public enum AccessState { AVAILABLE, CONDITIONAL, UNKNOWN }
}
