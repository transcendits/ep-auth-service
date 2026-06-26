package com.ep.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.KeyPair;
import org.junit.jupiter.api.Test;

class PemKeyUtilsTest {
    @Test
    void generatedKeysRoundTripThroughPem() {
        KeyPair keyPair = PemKeyUtils.generateRsaKeyPair();

        assertThat(PemKeyUtils.parsePrivate(PemKeyUtils.privatePem(keyPair.getPrivate())).getAlgorithm()).isEqualTo("RSA");
        assertThat(PemKeyUtils.parsePublic(PemKeyUtils.publicPem(keyPair.getPublic())).getAlgorithm()).isEqualTo("RSA");
    }
}
