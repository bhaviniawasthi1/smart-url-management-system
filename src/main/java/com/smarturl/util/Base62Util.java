package com.smarturl.util;

import com.smarturl.repository.ShortUrlRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Generates unique Base62 short codes for shortened URLs.
 *
 * Base62 uses the characters 0-9, A-Z, a-z (62 total), producing
 * 7-character codes that are compact and URL-safe.
 *
 * Collision detection: before returning a code, we query the database
 * via ShortUrlRepository.existsByShortCode(). If a collision is found,
 * we retry up to MAX_ATTEMPTS times.
 *
 * Viva Tip: Base62 is preferred over Base64 for URL shorteners because
 * it avoids '+' and '/' characters that require URL encoding. With 7
 * characters we get 62^7 ≈ 3.5 trillion possible codes — more than enough.
 */
@Component
public class Base62Util {

    private static final Logger log = LoggerFactory.getLogger(Base62Util.class);
    private static final String BASE62_ALPHABET =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int BASE62_RADIX = 62;
    private static final int CODE_LENGTH = 7;
    private static final int MAX_COLLISION_ATTEMPTS = 10;

    private final ShortUrlRepository shortUrlRepository;
    private final SecureRandom random = new SecureRandom();

    public Base62Util(ShortUrlRepository shortUrlRepository) {
        this.shortUrlRepository = shortUrlRepository;
    }

    /**
     * Generate a unique Base62 short code with collision detection.
     * Retries up to MAX_COLLISION_ATTEMPTS if the code already exists.
     *
     * @return a unique 7-character Base62 short code
     * @throws RuntimeException if a unique code cannot be generated
     */
    public String generateShortCode() {
        for (int attempt = 1; attempt <= MAX_COLLISION_ATTEMPTS; attempt++) {
            String code = generateRandomCode(CODE_LENGTH);
            if (!shortUrlRepository.existsByShortCode(code)) {
                return code;
            }
            log.debug("Short code collision on attempt {}/{}", attempt, MAX_COLLISION_ATTEMPTS);
        }
        log.warn("Failed to generate unique short code after {} attempts", MAX_COLLISION_ATTEMPTS);
        throw new RuntimeException("Unable to generate a unique short code. Please try again.");
    }

    /**
     * Generate a cryptographically random Base62 string of the given length.
     */
    private String generateRandomCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(BASE62_ALPHABET.charAt(random.nextInt(BASE62_RADIX)));
        }
        return sb.toString();
    }
}
