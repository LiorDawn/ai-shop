package org.example.aishop.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expire}")
    private Long expire;
public String createToken(String userId,String roleCode){
    //获取过期时间
    Date now =new Date();
    Date expireDate =new Date(now.getTime()+expire);
    Map<String,Object> header=new HashMap<>();
    header.put("alg","Hs256");
    header.put("typ","jwt");
    return JWT.create()
            .withHeader(header)
            .withClaim("userId",userId)
            .withClaim("roleCode",roleCode)
            .withIssuedAt(now)
            .withExpiresAt(expireDate)
            .sign(Algorithm.HMAC256(secret));
}


    //从 token 中获取用户 ID
    public String getUserIdFromToken(String token) {
        try {
            DecodedJWT jwt = JWT.decode(token);
            return jwt.getClaim("userId").asString();
        } catch (JWTDecodeException e) {
            return null;
        }
    }
    // 从token获取roleCode
    public String getRoleCodeFromToken(String token) {
        try {
            DecodedJWT jwt = JWT.decode(token);
            return jwt.getClaim("roleCode").asString();
        } catch (JWTDecodeException e) {
            return null;
        }
    }
//校验 token 是否有效
    public boolean validateToken(String token) {
        try {
            JWTVerifier verifier = JWT
            .require(Algorithm.HMAC256(secret))
            .build();
            verifier.verify(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            DecodedJWT jwt = JWT.decode(token);
            return jwt.getExpiresAt().before(new Date());
        } catch (JWTDecodeException e) {
            return true;
        }
    }
}