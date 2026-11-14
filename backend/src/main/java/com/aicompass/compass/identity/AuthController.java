package com.aicompass.compass.identity;

import jakarta.servlet.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.csrf.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;
import java.time.Clock;
import java.util.List;
import static com.aicompass.compass.identity.AuthDtos.*;

@RestController
@RequestMapping(value="/api/v1/auth",produces="application/json")
public class AuthController {
    private final IdentityService identity;
    private final IdentityJson json;
    private final AuthRateLimiter rates;
    private final SecurityContextRepository contexts;
    private final CsrfTokenRepository csrf;
    private final Clock clock;
    public AuthController(IdentityService identity,IdentityJson json,AuthRateLimiter rates,SecurityContextRepository contexts,CsrfTokenRepository csrf,Clock clock){
        this.identity=identity;this.json=json;this.rates=rates;this.contexts=contexts;this.csrf=csrf;this.clock=clock;
    }
    @GetMapping("/session")
    public SessionStatus session(java.security.Principal principal){return new SessionStatus(principal==null?null:identity.account(java.util.UUID.fromString(principal.getName())));}
    private Message generic(){return new Message("If the address is eligible, a message is available in the local email sink.");}
    @PostMapping(value="/register",consumes="application/json") @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(requestBody=@io.swagger.v3.oas.annotations.parameters.RequestBody(content=@Content(schema=@Schema(implementation=Credentials.class))))
    public Message register(@RequestBody String body){var input=json.read(body,Credentials.class);rates.take("register:"+IdentityService.normalize(input.email()),5);identity.register(input);return generic();}
    @PostMapping(value="/login",consumes="application/json")
    @Operation(requestBody=@io.swagger.v3.oas.annotations.parameters.RequestBody(content=@Content(schema=@Schema(implementation=Credentials.class))))
    public Account login(@RequestBody String body,HttpServletRequest req,HttpServletResponse res){
        var input=json.read(body,Credentials.class);rates.take("login:"+IdentityService.normalize(input.email()),10);
        var user=identity.login(input);
        var auth=UsernamePasswordAuthenticationToken.authenticated(user.id().toString(),null,List.of(new SimpleGrantedAuthority("ROLE_"+user.role())));
        new ChangeSessionIdAuthenticationStrategy().onAuthentication(auth,req,res);
        new CsrfAuthenticationStrategy(csrf).onAuthentication(auth,req,res);
        var context=SecurityContextHolder.createEmptyContext();context.setAuthentication(auth);SecurityContextHolder.setContext(context);
        req.getSession().setAttribute("credentialVersion",user.credentialVersion());req.getSession().setAttribute("authenticatedAt",clock.millis());
        contexts.saveContext(context,req,res);
        return identity.account(user.id());
    }
    @PostMapping(value="/verify",consumes="application/json")
    @Operation(requestBody=@io.swagger.v3.oas.annotations.parameters.RequestBody(content=@Content(schema=@Schema(implementation=TokenRequest.class))))
    public Message verify(@RequestBody String body){var in=json.read(body,TokenRequest.class);identity.consume(in.token(),"VERIFY",null);return new Message("Email verified. You can sign in.");}
    @PostMapping(value="/verification/resend",consumes="application/json") @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(requestBody=@io.swagger.v3.oas.annotations.parameters.RequestBody(content=@Content(schema=@Schema(implementation=EmailRequest.class))))
    public Message resend(@RequestBody String body){return requestEmail(body,"VERIFY");}
    @PostMapping(value="/password/forgot",consumes="application/json") @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(requestBody=@io.swagger.v3.oas.annotations.parameters.RequestBody(content=@Content(schema=@Schema(implementation=EmailRequest.class))))
    public Message forgot(@RequestBody String body){return requestEmail(body,"RESET");}
    private Message requestEmail(String body,String purpose){var in=json.read(body,EmailRequest.class);rates.take(purpose+":"+IdentityService.normalize(in.email()),5);identity.requestEmail(in.email(),purpose);return generic();}
    @PostMapping(value="/password/reset",consumes="application/json")
    @Operation(requestBody=@io.swagger.v3.oas.annotations.parameters.RequestBody(content=@Content(schema=@Schema(implementation=ResetRequest.class))))
    public Message reset(@RequestBody String body){var in=json.read(body,ResetRequest.class);identity.consume(in.token(),"RESET",in.password());return new Message("Password reset. Sign in again.");}
    @PostMapping("/logout") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(){throw new IllegalStateException("Spring Security LogoutFilter must handle this operation");}
}
