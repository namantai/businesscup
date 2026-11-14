package com.aicompass.compass.portfolio;

import com.aicompass.compass.catalog.CatalogReadService;
import com.aicompass.compass.identity.IdentityJson;
import com.aicompass.compass.recommendation.*;
import java.time.*;
import java.sql.Timestamp;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import static com.aicompass.compass.portfolio.PortfolioDtos.*;

@Service
@Transactional
public class PortfolioService {
    private final JdbcTemplate jdbc;
    private final IdentityJson json;
    private final ProfileValidator validator;
    private final CatalogReadService catalog;
    private final Clock clock;
    public PortfolioService(JdbcTemplate jdbc,IdentityJson json,ProfileValidator validator,CatalogReadService catalog,Clock clock){this.jdbc=jdbc;this.json=json;this.validator=validator;this.catalog=catalog;this.clock=clock;}
    private void lockOwner(UUID owner){
        if(jdbc.queryForList("SELECT id FROM compass.user_account WHERE id=? AND verified_at IS NOT NULL FOR UPDATE",UUID.class,owner).isEmpty())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Authentication required");
    }
    private static ResponseStatusException missing(){return new ResponseStatusException(HttpStatus.NOT_FOUND,"Record not found");}
    public List<SavedProfile> profiles(UUID owner){
        return jdbc.query("SELECT * FROM compass.saved_profile WHERE owner_id=? ORDER BY created_at,id",(r,n)->new SavedProfile(r.getObject("id",UUID.class),r.getString("name"),json.read(r.getString("profile_json"),RecommendationProfile.class),r.getLong("revision"),r.getTimestamp("created_at").toInstant(),r.getTimestamp("updated_at").toInstant()),owner);
    }
    public SavedProfile profile(UUID owner,UUID id){return profiles(owner).stream().filter(p->p.id().equals(id)).findFirst().orElseThrow(PortfolioService::missing);}
    public SavedProfile create(UUID owner,ProfileWrite input){
        lockOwner(owner);validator.read(json.write(input.profile()),clock.instant());
        if(input.revision()!=0)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"New profile revision must be zero");
        if(!profiles(owner).isEmpty())throw new ResponseStatusException(HttpStatus.CONFLICT,"Free accounts allow one saved profile");
        UUID id=UUID.randomUUID();var now=Timestamp.from(clock.instant());
        jdbc.update("INSERT INTO compass.saved_profile(id,owner_id,name,profile_json,created_at,updated_at) VALUES (?,?,?,CAST(? AS jsonb),?,?)",id,owner,input.name().strip(),json.write(input.profile()),now,now);
        return profile(owner,id);
    }
    public SavedProfile update(UUID owner,UUID id,ProfileWrite input){
        lockOwner(owner);profile(owner,id);validator.read(json.write(input.profile()),clock.instant());
        int changed=jdbc.update("UPDATE compass.saved_profile SET name=?,profile_json=CAST(? AS jsonb),revision=revision+1,updated_at=? WHERE id=? AND owner_id=? AND revision=?",input.name().strip(),json.write(input.profile()),Timestamp.from(clock.instant()),id,owner,input.revision());
        if(changed!=1)throw new ResponseStatusException(HttpStatus.CONFLICT,"Profile changed; reload before saving");
        return profile(owner,id);
    }
    public void deleteProfile(UUID owner,UUID id){if(jdbc.update("DELETE FROM compass.saved_profile WHERE id=? AND owner_id=?",id,owner)!=1)throw missing();}
    public List<SavedRecommendation> recommendations(UUID owner){
        return jdbc.query("SELECT * FROM compass.recommendation_snapshot WHERE owner_id=? ORDER BY saved_at DESC,id",(r,n)->new SavedRecommendation(r.getObject("id",UUID.class),json.read(r.getString("profile_json"),RecommendationProfile.class),json.read(r.getString("result_json"),RecommendationResult.class),r.getTimestamp("saved_at").toInstant()),owner);
    }
    public SavedRecommendation recommendation(UUID owner,UUID id){return recommendations(owner).stream().filter(r->r.id().equals(id)).findFirst().orElseThrow(PortfolioService::missing);}
    public SavedRecommendation save(UUID owner,SnapshotWrite input){
        lockOwner(owner);
        if(recommendations(owner).size()>=100)throw new ResponseStatusException(HttpStatus.CONFLICT,"Saved result limit reached; delete an old result");
        if(!RecommendationEngine.ENGINE_VERSION.equals(input.engineVersion()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Engine changed; run a new preview");
        // Reproduce with the recorded time/version, never accept client prices/ranking as facts.
        var node=tools.jackson.databind.json.JsonMapper.builder().build().valueToTree(input.profile());
        ((tools.jackson.databind.node.ObjectNode)node).put("asOf",input.asOf().toString());
        var profile=validator.read(node.toString(),clock.instant());
        var result=new RecommendationEngine().calculate(profile,catalog.snapshot(input.catalogVersion()),input.asOf());
        if(!result.policyVersion().equals(input.policyVersion()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Policy changed; run a new preview");
        UUID id=UUID.randomUUID();result=result.withRequestId(id.toString());
        var now=clock.instant().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        jdbc.update("INSERT INTO compass.recommendation_snapshot VALUES (?,?,CAST(? AS jsonb),CAST(? AS jsonb),?,?,?,?,?)",id,owner,json.write(profile),json.write(result),result.catalogVersion(),result.engineVersion(),result.policyVersion(),Timestamp.from(result.asOf()),Timestamp.from(now));
        return new SavedRecommendation(id,profile,result,now);
    }
    public void deleteRecommendation(UUID owner,UUID id){if(jdbc.update("DELETE FROM compass.recommendation_snapshot WHERE id=? AND owner_id=?",id,owner)!=1)throw missing();}
    public List<SavedStack> stacks(UUID owner){return jdbc.query("SELECT * FROM compass.saved_stack WHERE owner_id=? ORDER BY created_at,id",(r,n)->new SavedStack(r.getObject("id",UUID.class),r.getObject("snapshot_id",UUID.class),r.getString("bundle_id"),r.getTimestamp("created_at").toInstant()),owner);}
    public SavedStack stack(UUID owner,StackWrite input){
        lockOwner(owner);var snapshot=recommendation(owner,input.snapshotId());
        if(java.util.stream.Stream.concat(snapshot.result().bundles().stream(),snapshot.result().conditionalAlternatives().stream()).noneMatch(b->b.id().equals(input.bundleId())))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Bundle is not part of this result");
        UUID id=UUID.randomUUID();var now=clock.instant().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        jdbc.update("INSERT INTO compass.saved_stack VALUES (?,?,?,?,?) ON CONFLICT(snapshot_id,bundle_id) DO NOTHING",id,owner,input.snapshotId(),input.bundleId(),Timestamp.from(now));
        return stacks(owner).stream().filter(s->s.snapshotId().equals(input.snapshotId())&&s.bundleId().equals(input.bundleId())).findFirst().orElseThrow();
    }
    public void deleteStack(UUID owner,UUID id){if(jdbc.update("DELETE FROM compass.saved_stack WHERE id=? AND owner_id=?",id,owner)!=1)throw missing();}
}
