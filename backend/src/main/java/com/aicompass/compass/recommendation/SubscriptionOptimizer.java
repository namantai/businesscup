package com.aicompass.compass.recommendation;

import com.aicompass.compass.catalog.*;
import com.aicompass.compass.shared.time.RenewalSchedule;
import java.math.*;
import java.time.*;
import java.util.*;
import static com.aicompass.compass.recommendation.OptimizationResult.*;
import static com.aicompass.compass.recommendation.RecommendationResult.*;
import static com.aicompass.compass.recommendation.CostCalculator.*;

/** Ledger comparison composes the existing solver; it does not infer quality or execute cancellations. */
public final class SubscriptionOptimizer {
    public static final String VERSION="p07-1";
    public OptimizationResult calculate(RecommendationProfile input,Map<String,SubscriptionTerms> entries,PublishedCatalogSnapshot catalog,Instant at) {
        entries=new TreeMap<>(entries);
        if(catalog==null)throw ProfileValidator.bad("Publish a catalog before comparing subscriptions");
        var existing=new ArrayList<RecommendationProfile.ExistingSubscription>();
        var limitations=new ArrayList<String>(List.of("Проекция указанных условий, не гарантия будущих цен или экономии.",
                "Уже оплаченные суммы не возвращаются; отмена и покупки выполняются пользователем у поставщика.",
                "Пользовательские цены, даты и отмена — USER_REPORTED, не проверка поставщиком.",
                "Функциональное пересечение не доказывает взаимозаменяемость, качество или использование всех функций.",
                "Альтернативы ограничены каталогом и стратегиями существующего движка; глобальный минимум расходов не заявляется."));
        for(var e:entries.entrySet()) {
            var t=e.getValue(); if(t.status()==SubscriptionTerms.Status.ENDED)continue;
            boolean linked=catalog.offers().stream().anyMatch(o->o.id().equals(t.offerId()) && o.planId().equals(t.planId()));
            var today=at.atZone(ZoneId.of(t.billingZone())).toLocalDate();
            var anchor=t.renewalAnchor()==null?today:t.renewalAnchor();
            var next=new RenewalSchedule(anchor,ZoneId.of(t.billingZone()),t.billingPeriod()==RecommendationProfile.Period.YEAR?12:1)
                    .nextOnOrAfter(today.atStartOfDay(ZoneId.of(t.billingZone())).toInstant()).atZone(ZoneId.of(t.billingZone())).toLocalDate();
            existing.add(new RecommendationProfile.ExistingSubscription(e.getKey(),linked?t.planId():null,linked?t.offerId():null,t.currency(),
                    decimal(add(number(t.amount()),number(t.additionalPerPeriod()))),t.billingPeriod(),anchor,next,t.billingZone(),t.mustKeep()));
            if(t.planId()!=null && !linked)limitations.add(e.getKey()+": отсутствует в выбранном каталоге; функции UNKNOWN.");
        }
        var p=withExisting(input,existing,at);
        var evidence=new EvidenceAssessment(catalog,at);var coverage=new CapabilityCoverage(catalog,p,evidence);
        var costs=new CostCalculator(catalog,evidence,p,at);
        var timelines=new LinkedHashMap<String,Timeline>();var overrides=new HashMap<String,CostCalculator.Amounts>();
        entries.forEach((id,t)->{
            var time=costs.ledger(id,t,false);timelines.put(id,time);
            var today=at.atZone(ZoneId.of(t.billingZone())).toLocalDate();
            boolean prepaidHorizon=t.paidUntil()!=null && !t.paidUntil().isBefore(today.plusMonths(p.horizonMonths()));
            boolean sustainable=t.status()==SubscriptionTerms.Status.ACTIVE
                    && (t.paidFrom()==null || !t.paidFrom().isAfter(today))
                    && (t.autoRenew()==SubscriptionTerms.Renewal.YES || t.autoRenew()==SubscriptionTerms.Renewal.NO && prepaidHorizon)
                    && t.obligation()!=SubscriptionTerms.Obligation.UNKNOWN;
            overrides.put(id,new CostCalculator.Amounts(number(time.recurringMonthly()),number(time.cashDueNow()),number(time.cashDueNow()),number(time.horizonTotal()),
                    sustainable && time.recurringMonthly()!=null && time.horizonTotal()!=null,
                    List.of(reason(Code.USER_SUPPLIED_COST,"Ledger terms and charges reported by the user; not provider-verified")),List.of()));
        });
        var resolved=CandidateResolver.resolve(catalog,p,evidence,coverage,costs,at,overrides);
        var currentCoverage=coverage.combine(resolved.existing());
        boolean required=currentCoverage.stream().allMatch(c->!c.required()||c.confirmed()||c.acceptedForRequirement());
        var usage=new UsageAssessment(catalog,evidence,coverage,at).assess(resolved.existing(),p);
        required &= usage.hardFit();
        required &= resolved.existing().stream().allMatch(c->c.eligibility==Eligibility.ELIGIBLE);
        var currentCosts=sum(timelines.values());
        required &= resolved.existing().size()<=p.maxTools();
        for(int i=0;i<resolved.existing().size();i++)for(int j=0;j<i;j++)
            if(BundleSearch.incompatible(resolved.existing().get(i),resolved.existing().get(j)))required=false;
        required &= currentCosts.horizonTotal()!=null && currentCosts.recurringMonthly()!=null && number(currentCosts.recurringMonthly()).compareTo(number(p.budget().monthlyMax()))<=0
                && currentCosts.cashDueNow()!=null && number(currentCosts.cashDueNow()).compareTo(number(p.budget().upfrontMax()))<=0;
        var current=new Scenario("current","CURRENT",List.copyOf(entries.keySet()),resolved.existing().stream().map(c->new Item(c.id,c.plan==null?null:c.plan.productId(),c.plan==null?null:c.plan.id(),c.offer==null?null:c.offer.id(),Action.KEEP,c.existing.currency(),c.existing.amount(),c.existing.billingPeriod().name(),c.existing.nextRenewal(),c.agents,c.models,c.pools,c.states.stream().flatMap(state->state.evidenceIds().stream()).distinct().sorted().toList())).toList(),currentCosts,currentCoverage,required,List.of(),
                entries.keySet().stream().map(id->new PortfolioAction(id,Action.KEEP,"Сохранить текущие условия продления")).toList(),null,null,"BASELINE",List.of());
        var result=new RecommendationEngine().calculate(p,catalog,at,overrides);
        var alternatives=new ArrayList<Scenario>();
        for(var bundle:result.bundles()) {
            var kept=new HashSet<String>();bundle.items().stream().filter(i->i.action()==Action.KEEP).forEach(i->kept.add(i.id().substring("existing:".length())));
            var parts=new ArrayList<Timeline>();var actions=new ArrayList<PortfolioAction>();
            var conditions=new ArrayList<String>();boolean permitted=true;
            for(var e:entries.entrySet()) {
                var t=e.getValue();boolean keep=kept.contains(e.getKey())||t.status()==SubscriptionTerms.Status.ENDED;
                if(!keep && (t.mustKeep()||p.portfolioMode()==RecommendationProfile.PortfolioMode.AUGMENT))permitted=false;
                var time=keep?timelines.get(e.getKey()):costs.ledger(e.getKey(),t,true);parts.add(time);
                actions.add(new PortfolioAction(e.getKey(),keep?Action.KEEP:t.cancellationAvailableFrom()==null?Action.REVIEW:Action.CANCEL_AT_RENEWAL,
                        keep?"Сохранить запись и условия": "Отмена не ранее "+(t.cancellationAvailableFrom()==null?"UNKNOWN":t.cancellationAvailableFrom())+", обязательство: "+t.obligation()+" "+(t.commitmentUntil()==null?"":t.commitmentUntil())+"; возврат не предполагается"));
                if(!keep)conditions.add(t.name()+": оплаченный доступ до "+(t.paidUntil()==null?"UNKNOWN":t.paidUntil())+"; переходные расходы учтены отдельно. Новый набор приобретается сейчас, возможен оплаченный период пересечения.");
            }
            for(var item:bundle.items()) if(item.action()==Action.ADD) {
                var offer=catalog.offers().stream().filter(o->o.id().equals(item.offerId())).findFirst().orElseThrow();
                parts.add(costs.newPurchase(item.id(),offer));
                actions.add(new PortfolioAction(item.id(),Action.ADD,"Новая покупка сейчас; полный upfront, без автоматической покупки"));
            }
            // Same server policy as RecommendationEngine; client cannot inject a Compass fee.
            var fee=number(bundle.costs().compassFeeMonthly());
            parts.add(new Timeline("0",decimal(fee),decimal(fee),decimal(fee==null?null:fee.multiply(BigDecimal.valueOf(p.horizonMonths()))),"0","0",null,List.of(),List.of()));
            var total=sum(parts);
            if(total.cashDueNow()!=null && number(total.cashDueNow()).compareTo(number(p.budget().upfrontMax()))>0)permitted=false;
            if(total.recurringMonthly()!=null && number(total.recurringMonthly()).compareTo(number(p.budget().monthlyMax()))>0)permitted=false;
            var lost=catalog.capabilities().stream().filter(cap->resolved.existing().stream().anyMatch(c->c.plan!=null&&coverage.state(c.plan.productId(),c.plan.id(),cap.id()).confirmed()))
                    .filter(cap->bundle.items().stream().noneMatch(item->coverage.state(item.productId(),item.planId(),cap.id()).confirmed())).map(CatalogManifest.Capability::id).sorted().toList();
            String savings=current.costs().horizonTotal()==null||total.horizonTotal()==null?null:decimal(number(current.costs().horizonTotal()).subtract(number(total.horizonTotal())));
            if(!permitted)continue;
            boolean completeBudget=total.horizonTotal()!=null && total.cashDueNow()!=null && total.recurringMonthly()!=null;
            alternatives.add(new Scenario(bundle.id(),"OPTIMIZED",kept.stream().sorted().toList(),bundle.items(),total,bundle.coverage(),completeBudget,lost,actions,
                    savings,null,savings==null?"UNKNOWN":"ESTIMATED_USER_REPORTED",conditions));
        }
        alternatives.sort(Comparator.comparing((Scenario s)->s.costs().horizonTotal()==null?null:number(s.costs().horizonTotal()),Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(Scenario::id));
        boolean cheaper=alternatives.stream().anyMatch(s->s.requirementsMet()&&s.potentialSavings()!=null&&number(s.potentialSavings()).signum()>0);
        boolean incomplete=current.costs().horizonTotal()==null || alternatives.stream().anyMatch(s->s.costs().horizonTotal()==null);
        String decision=cheaper?"CONSIDER_CHANGE":incomplete?"UNKNOWN":required?"KEEP_CURRENT":"REVIEW_REQUIREMENTS";
        var overlaps=new ArrayList<Overlap>();var candidates=resolved.existing();
        for(int i=0;i<candidates.size();i++)for(int j=i+1;j<candidates.size();j++){
            var a=candidates.get(i);var b=candidates.get(j);var functions=new ArrayList<String>();var ids=new TreeSet<String>();
            if(a.plan!=null && b.plan!=null)for(var cap:catalog.capabilities()) {
                var left=coverage.state(a.plan.productId(),a.plan.id(),cap.id());var right=coverage.state(b.plan.productId(),b.plan.id(),cap.id());
                if(left.confirmed()&&right.confirmed()){functions.add(cap.id());ids.addAll(left.evidenceIds());ids.addAll(right.evidenceIds());}
            }
            String status=functions.isEmpty()?"INSUFFICIENT_DATA":"CONFIRMED_FUNCTIONAL_OVERLAP";
            if(a.mandatory||b.mandatory)status="BLOCKED_BY_REQUIREMENTS";
            overlaps.add(new Overlap(a.existing.id(),b.existing.id(),status,functions,List.copyOf(ids),"Общие подтверждённые функции, не процент дублирования. Возможность замены проверяется только полным набором, включая ограничения и общие квоты."));
        }
        // P04 monthly savings/actions do not describe the ledger timeline. Expose its evidence,
        // assessments and search metadata, while scenarios above are the sole financial comparison.
        var context=new RecommendationResult(null,result.catalogVersion(),result.datasetKind(),result.engineVersion(),result.policyVersion(),result.asOf(),result.searchComplete(),result.search(),result.status(),List.of(),List.of(),result.unmetRequirements(),result.warnings(),result.assessments(),result.evidence(),result.sources(),result.fxSnapshots());
        return new OptimizationResult(catalog.version(),VERSION,at,p.horizonMonths(),p.budget().currency(),input,Collections.unmodifiableMap(new TreeMap<>(entries)),decision,current,List.copyOf(alternatives),overlaps,context,
                java.util.stream.Stream.concat(result.fxSnapshots().stream(),costs.ledgerFx.stream()).distinct().sorted(Comparator.comparing(FxUsed::id)).toList(),limitations);
    }
    private static Timeline sum(Collection<Timeline> values) {
        BigDecimal paid=BigDecimal.ZERO,now=BigDecimal.ZERO,monthly=BigDecimal.ZERO,total=BigDecimal.ZERO,mandatory=BigDecimal.ZERO,transition=BigDecimal.ZERO;
        var charges=new ArrayList<Charge>();var unknowns=new ArrayList<String>();
        for(var v:values){paid=add(paid,number(v.paidAmount()));now=add(now,number(v.cashDueNow()));monthly=add(monthly,number(v.recurringMonthly()));total=add(total,number(v.horizonTotal()));mandatory=add(mandatory,number(v.mandatoryTotal()));transition=add(transition,number(v.transitionCost()));charges.addAll(v.charges());unknowns.addAll(v.unknowns());}
        charges.sort(Comparator.comparing(Charge::date).thenComparing(Charge::subscriptionId));
        return new Timeline(decimal(paid),decimal(now),decimal(monthly),decimal(total),decimal(mandatory),decimal(transition),charges.isEmpty()?null:charges.getFirst().date(),charges,unknowns);
    }
    private static RecommendationProfile withExisting(RecommendationProfile p,List<RecommendationProfile.ExistingSubscription> entries,Instant at){
        return new RecommendationProfile(p.profileSchemaVersion(),p.country(),p.interfaceLanguage(),p.outputLanguages(),p.needs(),p.budget(),p.allowAnnualBilling(),p.maxTools(),p.horizonMonths(),p.priority(),p.studentStatus(),p.portfolioMode(),p.purchaseChannel(),p.requirements(),entries,p.acceptedConditionEvidenceIds(),at);
    }
}
