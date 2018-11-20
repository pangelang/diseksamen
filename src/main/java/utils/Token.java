package utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.gson.Gson;
import controllers.UserController;
import model.User;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public final class Token {

    private String token;

    public Token(String token){
        this.token = token;
    }

    private static Date expirationDate() {
        //Kilde: https://www.owasp.org/index.php/JSON_Web_Token_(JWT)_Cheat_Sheet_for_Java
        Calendar c = Calendar.getInstance();
        Date now = c.getTime();
        c.add(Calendar.MINUTE, 15);
        Date expirationDate = c.getTime();

        return expirationDate;
    }

    public static String createToken(User user) {

        try {
            Algorithm algorithm = Algorithm.HMAC256("secret");
            String token = JWT.create()
                    .withIssuer("auth0")
                    .withExpiresAt(expirationDate())
                    .withClaim("sub", user.getId())
                    //.withIssuedAt(now)
                    //.withNotBefore(now)
                    .sign(algorithm);

            return token;

        } catch (JWTCreationException exception) {
            //Invalid Signing configuration / Couldn't convert Claims.
        }
        return null;
    }

    public static DecodedJWT verifyToken(String token, int idUser) {

        try {
            Algorithm algorithm = Algorithm.HMAC256("secret");
            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer("auth0")
                    .build(); //Reusable verifier instance
            DecodedJWT jwt = verifier.verify(token);

            String check = new Gson().toJson(jwt);

            if (check.contains("\"subject\":\"" + idUser + "\"")) {

                return jwt;
            }

        } catch (JWTVerificationException exception){
            //Invalid signature/claims
        }
        return null;
    }

    public String getToken() {
        return token;
    }

}
