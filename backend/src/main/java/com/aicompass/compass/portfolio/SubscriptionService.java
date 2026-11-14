package com.aicompass.compass.portfolio;

import com.aicompass.compass.catalog.*;
import com.aicompass.compass.identity.IdentityJson;
import com.aicompass.compass.recommendation.*;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import static com.aicompass.compass.portfolio.SubscriptionDtos.*;

@Service
@Transactional
public class SubscriptionService {
    private final JdbcTemplate jdbc;
    private final IdentityJson json;
    private final CatalogReadService catalog;
    private final ProfileValidator validator;
    private final Clock clock;
    public SubscriptionService(JdbcTemplate jdbc,IdentityJson json,CatalogReadService catalog,ProfileValidator validator,Clock clock){this.jdbc=jdbc;this.json=json;this.catalog=catalog;this.validator=validator;this.clock=clock;}
    private static ResponseStatusException error(HttpStatus code,String text){return new ResponseStatusException(code,text);}
    private void lock(UUID owner){if(jdbc.queryForList("SELECT id FROM compass.user_account WHERE id=? AND verified_at IS NOT NULL FOR UPDATE",UUID.class,owner).isEmpty())throw error(HttpStatus.UNAUTHORIZED,"Authentication required");}
    public List<SubscriptionEntry> subscriptions(UUID owner){return jdbc.query("SELECT * FROM compass.user_subscription WHERE owner_id=? ORDER BY created_at,id",(r,n)->new SubscriptionEntry(r.getObject("id",UUID.class),json.read(r.getString("terms_json"),SubscriptionTerms.class),r.getLong("revision"),r.getTimestamp("created_at").toInstant(),r.getTimestamp("updated_at").toInstant()),owner);}
    public SubscriptionEntry subscription(UUID owner,UUID id){return subscriptions(owner).stream().filter(s->s.id().equals(id)).findFirst().orElseThrow(()->error(HttpStatus.NOT_FOUND,"Subscription not found"));}
    public SubscriptionEntry write(UUID owner,UUID id,SubscriptionWrite input){
        lock(owner);var existing=subscriptions(owner);if(id!=null)subscription(owner,id);
        var t=input.terms();t.validate();
        if(existing.stream().anyMatch(e->!e.id().equals(id)&&(e.terms().name().equals(t.name())||t.planId()!=null&&t.planId().equals(e.terms().planId()))))throw error(HttpStatus.CONFLICT,"Subscription already recorded");
        if(id==null && (input.revision()!=0 || existing.size()>=8))throw error(HttpStatus.CONFLICT,"At most eight subscriptions; new revision must be zero");
        if(t.catalogVersion()!=null){
            var snap=catalog.snapshot(t.catalogVersion());
            var offer=snap.offers().stream().filter(o->o.id().equals(t.offerId())&&o.planId().equals(t.planId())).findFirst().orElseThrow(()->error(HttpStatus.BAD_REQUEST,"Invalid catalog reference"));
            if(t.priceOrigin()==SubscriptionTerms.PriceOrigin.CATALOG){
                var claim=snap.evidence().stream().filter(e->e.id().equals(offer.priceEvidenceId())).findFirst().orElseThrow();
                if(!Objects.equals(offer.amount(),t.amount()) || !offer.currency().equals(t.currency()) || !offer.billingPeriod().name().equals(t.billingPeriod().name())
                        || !(claim.status().equals("VERIFIED_SOURCE") || snap.datasetKind().equals("DEMO")&&claim.status().equals("DEMO"))
                        || Duration.between(claim.verifiedAt(),clock.instant()).compareTo(Duration.ofDays(7))>=0)
                    throw error(HttpStatus.BAD_REQUEST,"Catalog price not confirmed; enter actual price as USER_REPORTED or UNKNOWN");
            }
        }
        var now=Timestamp.from(clock.instant());UUID key=id==null?UUID.randomUUID():id;
        if(id==null)jdbc.update("INSERT INTO compass.user_subscription(id,owner_id,name,catalog_version,plan_id,offer_id,currency,amount,price_state,price_origin,terms_json,created_at,updated_at) VALUES (?,?,?,?,?,?,?,CAST(? AS numeric),?,?,CAST(? AS jsonb),?,?)",key,owner,t.name(),t.catalogVersion(),t.planId(),t.offerId(),t.currency(),t.amount(),t.amount()==null?"UNKNOWN":"KNOWN",t.priceOrigin().name(),json.write(t),now,now);
        else if(jdbc.update("UPDATE compass.user_subscription SET name=?,catalog_version=?,plan_id=?,offer_id=?,currency=?,amount=CAST(? AS numeric),price_state=?,price_origin=?,terms_json=CAST(? AS jsonb),revision=revision+1,updated_at=? WHERE id=? AND owner_id=? AND revision=?",t.name(),t.catalogVersion(),t.planId(),t.offerId(),t.currency(),t.amount(),t.amount()==null?"UNKNOWN":"KNOWN",t.priceOrigin().name(),json.write(t),now,id,owner,input.revision())!=1)throw error(HttpStatus.CONFLICT,"Subscription changed; reload before saving");
        return subscription(owner,key);
    }
    public void delete(UUID owner,UUID id,long revision){lock(owner);subscription(owner,id);if(jdbc.update("DELETE FROM compass.user_subscription WHERE id=? AND owner_id=? AND revision=?",id,owner,revision)!=1)throw error(HttpStatus.CONFLICT,"Subscription changed; reload before deleting");}
    @Transactional(isolation=Isolation.REPEATABLE_READ,readOnly=true)
    public OptimizationResult optimize(UUID owner,OptimizationRequest input){
        var entries=subscriptions(owner);var versions=new TreeMap<UUID,Long>();entries.forEach(e->versions.put(e.id(),e.revision()));
        if(!versions.equals(input.revisions()))throw error(HttpStatus.CONFLICT,"Subscription ledger changed; reload before comparing");
        var p=validator.read(json.write(input.profile()),clock.instant());
        if(!p.existingSubscriptions().isEmpty())throw error(HttpStatus.BAD_REQUEST,"Saved ledger is authoritative; profile existingSubscriptions must be empty");
        var snap=catalog.currentSnapshot();
        if(snap==null||!snap.version().equals(input.catalogVersion()))throw error(HttpStatus.CONFLICT,"Catalog changed; reload before comparing");
        var terms=new LinkedHashMap<String,SubscriptionTerms>();entries.forEach(e->terms.put(e.id().toString(),e.terms()));
        return new SubscriptionOptimizer().calculate(p,terms,snap,p.asOf()==null?clock.instant():p.asOf());
    }
}
