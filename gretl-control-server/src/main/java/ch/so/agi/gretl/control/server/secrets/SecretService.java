package ch.so.agi.gretl.control.server.secrets;

import ch.so.agi.gretl.control.server.persistence.SecretRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SecretService {
    private final SecretRepository repository;
    private final SecretCrypto crypto;

    public SecretService(SecretRepository repository, SecretCrypto crypto) {
        this.repository = repository;
        this.crypto = crypto;
    }

    public void put(String name, String value) {
        repository.put(name, crypto.encrypt(value), Instant.now());
    }

    public Map<String, String> resolve(List<String> names) {
        Map<String, String> values = new LinkedHashMap<>();
        for (String name : names) {
            String cipherText = repository.findCipherText(name)
                    .orElseThrow(() -> new IllegalArgumentException("Secret '" + name + "' is not configured."));
            values.put(name, crypto.decrypt(cipherText));
        }
        return values;
    }

    public List<String> names() {
        return repository.findNames();
    }
}
