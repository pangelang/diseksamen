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

    public static String createToken(User user) {

        try {
            Algorithm algorithm = Algorithm.HMAC256("secret");
            String token = JWT.create()
                    .withIssuer("auth0")
                    .withIssuedAt(new Date (System.currentTimeMillis()))
                    .withExpiresAt(new Date (System.currentTimeMillis() + 900000)) //15 min. duration
                    .withSubject(Integer.toString(user.getId()))
                    .sign(algorithm);

            return token;

        } catch (JWTCreationException exception) {
        }
        return null;
    }

    public static boolean verifyToken(String token, User user) {

        try {
            Algorithm algorithm = Algorithm.HMAC256("secret");
            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer("auth0")
                    .withSubject(Integer.toString(user.getId()))
                    .build();

            verifier.verify(token);

            return true;

        } catch (JWTVerificationException exception) {
        }
        return false;
    }
}
