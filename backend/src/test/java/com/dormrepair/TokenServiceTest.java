package com.dormrepair;
// AI-assisted: tests reject forged, expired and malformed JWTs.
import com.dormrepair.support.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class TokenServiceTest {
    private static final String SECRET="test-only-32-byte-minimum-secret-value";
    private final CurrentUser user=new CurrentUser(1L,"student","学生","STUDENT");
    @Test void roundTrip() { var s=new TokenService(SECRET,1); var t=s.createToken(user); assertEquals(3,t.split("\\.").length); assertEquals(1L,s.parse(t).userId());assertEquals("STUDENT",s.parse(t).roleCode()); }
    @Test void rejectsDifferentSigningKey() { var t=new TokenService(SECRET,1).createToken(user);assertThrows(BusinessException.class,()->new TokenService(SECRET+"other",1).parse(t)); }
    @Test void rejectsExpired() throws Exception {
        var now=java.time.Instant.now();
        var claims=new com.nimbusds.jwt.JWTClaimsSet.Builder().issuer("dorm-repair").subject("1")
            .issueTime(java.util.Date.from(now.minusSeconds(7200))).expirationTime(java.util.Date.from(now.minusSeconds(3600)))
            .claim("username","student").claim("role","STUDENT").build();
        var jwt=new com.nimbusds.jwt.SignedJWT(new com.nimbusds.jose.JWSHeader(com.nimbusds.jose.JWSAlgorithm.HS256),claims);
        jwt.sign(new com.nimbusds.jose.crypto.MACSigner(SECRET));
        var s=new TokenService(SECRET,1);assertThrows(BusinessException.class,()->s.parse(jwt.serialize()));
    }
    @Test void rejectsMalformed() {var s=new TokenService(SECRET,1);for(String t:new String[]{"", "abc.def", "a.b.c"}) assertThrows(BusinessException.class,()->s.parse(t));}
    @Test void rejectsWeakKey() {assertThrows(IllegalArgumentException.class,()->new TokenService("weak",1));}
}
