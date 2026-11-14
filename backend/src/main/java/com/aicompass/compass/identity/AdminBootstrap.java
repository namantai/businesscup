package com.aicompass.compass.identity;

import java.util.UUID;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name="identity-admin")
public class AdminBootstrap implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    public AdminBootstrap(JdbcTemplate jdbc){this.jdbc=jdbc;}
    @Override @Transactional public void run(ApplicationArguments args){
        UUID id=UUID.fromString(args.getOptionValues("identity-admin").getFirst());
        jdbc.execute("LOCK TABLE compass.user_account IN EXCLUSIVE MODE");
        if(jdbc.queryForObject("SELECT count(*) FROM compass.user_account WHERE role='ADMIN'",Integer.class)!=0)
            throw new IllegalStateException("Initial administrator already exists");
        if(jdbc.update("UPDATE compass.user_account SET role='ADMIN',credential_version=credential_version+1,updated_at=now() WHERE id=? AND verified_at IS NOT NULL",id)!=1)
            throw new IllegalStateException("A verified account is required");
        jdbc.update("DELETE FROM compass.spring_session WHERE principal_name=?",id.toString());
        System.out.println("Initial administrator granted; sign in again. No public admin operations enabled.");
    }
}
