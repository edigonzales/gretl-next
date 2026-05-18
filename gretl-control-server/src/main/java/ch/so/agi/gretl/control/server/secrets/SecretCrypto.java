package ch.so.agi.gretl.control.server.secrets;

import ch.so.agi.gretl.control.server.config.GretlControlProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class SecretCrypto {
    private static final Logger LOGGER = LoggerFactory.getLogger(SecretCrypto.class);
    private static final int NONCE_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecureRandom secureRandom = new SecureRandom();
    private final SecretKeySpec key;

    public SecretCrypto(GretlControlProperties properties) {
        String configuredKey = properties.getSecrets().getMasterKey();
        if (configuredKey == null || configuredKey.isBlank()) {
            LOGGER.warn("GRETL_CONTROL_SECRET_KEY is not configured. Using a development-only secret encryption key.");
            configuredKey = "gretl-control-development-key";
        }
        this.key = new SecretKeySpec(sha256(configuredKey), "AES");
    }

    public String encrypt(String plainText) {
        try {
            byte[] nonce = new byte[NONCE_BYTES];
            secureRandom.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, nonce));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[nonce.length + encrypted.length];
            System.arraycopy(nonce, 0, payload, 0, nonce.length);
            System.arraycopy(encrypted, 0, payload, nonce.length, encrypted.length);
            return Base64.getEncoder().encodeToString(payload);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Could not encrypt secret.", e);
        }
    }

    public String decrypt(String cipherText) {
        try {
            byte[] payload = Base64.getDecoder().decode(cipherText);
            byte[] nonce = java.util.Arrays.copyOfRange(payload, 0, NONCE_BYTES);
            byte[] encrypted = java.util.Arrays.copyOfRange(payload, NONCE_BYTES, payload.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, nonce));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("Could not decrypt secret.", e);
        }
    }

    private byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("SHA-256 is not available.", e);
        }
    }
}
