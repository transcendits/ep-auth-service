package com.ep.auth.service;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public final class PemKeyUtils {
    private PemKeyUtils() {
    }

    public static KeyPair generateRsaKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("RSA is not available", ex);
        }
    }

    public static String publicPem(PublicKey key) {
        return pem("PUBLIC KEY", key.getEncoded());
    }

    public static String privatePem(PrivateKey key) {
        return pem("PRIVATE KEY", key.getEncoded());
    }

    public static RSAPublicKey parsePublic(String pem) {
        try {
            byte[] der = Base64.getDecoder().decode(strip(pem));
            return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
        } catch (Exception ex) {
            throw new IllegalStateException("Invalid RSA public key", ex);
        }
    }

    public static PrivateKey parsePrivate(String pem) {
        try {
            byte[] der = Base64.getDecoder().decode(strip(pem));
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (Exception ex) {
            throw new IllegalStateException("Invalid RSA private key", ex);
        }
    }

    private static String pem(String type, byte[] der) {
        return "-----BEGIN " + type + "-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(der)
                + "\n-----END " + type + "-----";
    }

    private static String strip(String pem) {
        return pem.replaceAll("-----BEGIN [A-Z ]+-----", "")
                .replaceAll("-----END [A-Z ]+-----", "")
                .replaceAll("\\s", "");
    }
}
