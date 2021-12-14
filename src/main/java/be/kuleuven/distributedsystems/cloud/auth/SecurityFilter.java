package be.kuleuven.distributedsystems.cloud.auth;

import be.kuleuven.distributedsystems.cloud.Utils;
import be.kuleuven.distributedsystems.cloud.entities.User;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.WebUtils;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;

@Component
public class SecurityFilter extends OncePerRequestFilter {

    @Autowired
    private WebClient.Builder webClientBuilder;

    @Autowired
    private String projectId;

    @Autowired
    private boolean isProduction;

    private String fireBasePubKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
//        logger.info("projectId: " + projectId);

        var session = WebUtils.getCookie(request, "session");
        if (session != null) {
            try {
                var token = session.getValue();
                var kid = JWT.decode(session.getValue());

                Base64.Decoder decoder = Base64.getDecoder();

                String header = new String(decoder.decode(kid.getHeader()));
                String payload = new String(decoder.decode(kid.getPayload()));

                var user_json = Utils.json_mapper.readTree(payload);
                var header_json = Utils.json_mapper.readTree(header);
                var email = user_json.get("email").asText();
                var user_id = user_json.get("user_id").asText();
                var roles = (user_json.has("roles"))?user_json.get("roles"): null;
//                logger.info("header: " + header_json.toString());
//                logger.info("payload: "  + user_json.toPrettyString());

                var role = "";

                if (roles != null && roles.isArray()) {
                    var rolesArray = (ArrayNode) roles;
                    for (var jsonNode : rolesArray) {
                        role = jsonNode.asText();
                        logger.info("role: " + role);
                        if (role.equals("manager")) {
                            break;
                        }
                    }
                }

                if (isProduction) {
                    var pubKeysJson = Utils.json_mapper.readTree(getPubKey());
                    var pubKey1 = pubKeysJson.get(header_json.get("kid").asText()).asText();
                    pubKey1 = pubKey1
                            .replace("-----BEGIN CERTIFICATE-----", "")
                            .replace("-----END CERTIFICATE-----", "")
                            .replace("\n","")
                    ;

                    logger.info("pub:" + pubKey1);


                    byte encodedCert[] = Base64.getDecoder().decode(pubKey1);
                    ByteArrayInputStream inputStream  =  new ByteArrayInputStream(encodedCert);

                    CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
                    X509Certificate cert = (X509Certificate)certFactory.generateCertificate(inputStream);
                    checkValid(cert);

                    Algorithm algorithm = Algorithm.RSA256((RSAPublicKey) cert.getPublicKey(), null);
                    DecodedJWT jwt = JWT. require(algorithm)
                            .withIssuer ("https://securetoken.google.com/" + projectId)
                            .build()
                            .verify(token);

//                    var email1 = jwt.getClaim("email");
//                    logger.info("email1: " + email1);
                }

                var user = new User(email, role, user_id);
                SecurityContext context = SecurityContextHolder.getContext();
                context.setAuthentication(new FirebaseAuthentication(user));
            } catch (Exception e) {
//                e.printStackTrace();
                logger.error(e.getMessage());
            }
        }
        filterChain.doFilter(request, response);
    }

    public void checkValid(X509Certificate cert) {
        try {
            cert.checkValidity();
        } catch (Exception e) {
            this.fireBasePubKey = requestPubKey();
        }
    }

    public String requestPubKey() {
        var pub = webClientBuilder
                .baseUrl("https://www.googleapis.com/robot/v1/metadata/x509/securetoken@system.gserviceaccount.com")
                .build()
                .get()
                .retrieve()
                .bodyToMono(String.class)
                .block();
        logger.info("retrieved pub:" + pub);
        return pub;
    }

    public String getPubKey() {
        if (this.fireBasePubKey == null) {
            this.fireBasePubKey = requestPubKey();
        }
        return this.fireBasePubKey;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return path.equals("/authenticate") || path.endsWith(".html") || path.endsWith(".js") || path.endsWith(".css");
    }

    private static class FirebaseAuthentication implements Authentication {
        private final User user;

        FirebaseAuthentication(User user) {
            this.user = user;
        }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            if (user.isManager()) {
                return List.of(new SimpleGrantedAuthority("manager"));
            } else{
                return new ArrayList<>();
            }
        }

        @Override
        public Object getCredentials() {
            return null;
        }

        @Override
        public Object getDetails() {
            return null;
        }

        @Override
        public User getPrincipal() {
            return this.user;
        }

        @Override
        public boolean isAuthenticated() {
            return true;
        }

        @Override
        public void setAuthenticated(boolean b) throws IllegalArgumentException {}

        @Override
        public String getName() {
            return user.getEmail();
        }
    }
}

