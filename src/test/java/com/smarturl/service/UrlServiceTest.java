package com.smarturl.service;

import com.smarturl.dto.ShortUrlRequest;
import com.smarturl.dto.ShortUrlResponse;
import com.smarturl.entity.Role;
import com.smarturl.entity.ShortUrl;
import com.smarturl.entity.User;
import com.smarturl.exception.ResourceNotFoundException;
import com.smarturl.repository.ClickEventRepository;
import com.smarturl.repository.ShortUrlRepository;
import com.smarturl.util.Base62Util;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UrlServiceTest {

    @Mock
    private ShortUrlRepository shortUrlRepository;
    @Mock
    private ClickEventRepository clickEventRepository;
    @Mock
    private Base62Util base62Util;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private UrlService urlService;
    private User testUser;

    @BeforeEach
    void setUp() {
        urlService = new UrlService(shortUrlRepository, clickEventRepository,
                base62Util, passwordEncoder, "http://localhost:8080");
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password(passwordEncoder.encode("password"))
                .role(Role.USER)
                .enabled(true)
                .build();
    }

    @Test
    void createShortUrl_ShouldReturnResponse() {
        when(base62Util.generateShortCode()).thenReturn("abc1234");
        when(shortUrlRepository.save(any(ShortUrl.class))).thenAnswer(invocation -> {
            ShortUrl su = invocation.getArgument(0);
            su.setId(1L);
            return su;
        });

        ShortUrlRequest request = ShortUrlRequest.builder()
                .url("https://example.com")
                .build();

        ShortUrlResponse response = urlService.createShortUrl(request, testUser);

        assertNotNull(response);
        assertEquals("abc1234", response.getShortCode());
        assertEquals("https://example.com", response.getLongUrl());
        assertEquals("http://localhost:8080/r/abc1234", response.getShortUrl());
        verify(shortUrlRepository).save(any(ShortUrl.class));
    }

    @Test
    void getLongUrlForRedirect_WhenDisabled_ShouldThrow() {
        ShortUrl shortUrl = ShortUrl.builder()
                .id(1L)
                .longUrl("https://example.com")
                .shortCode("abc1234")
                .user(testUser)
                .enabled(false)
                .build();

        when(shortUrlRepository.findByShortCode("abc1234")).thenReturn(Optional.of(shortUrl));

        assertThrows(ResourceNotFoundException.class,
                () -> urlService.getLongUrlForRedirect("abc1234", "127.0.0.1", null, null));
    }

    @Test
    void getLongUrlForRedirect_WhenEnabled_ShouldReturnUrl() {
        ShortUrl shortUrl = ShortUrl.builder()
                .id(1L)
                .longUrl("https://example.com")
                .shortCode("abc1234")
                .user(testUser)
                .enabled(true)
                .build();

        when(shortUrlRepository.findByShortCode("abc1234")).thenReturn(Optional.of(shortUrl));

        String result = urlService.getLongUrlForRedirect("abc1234", "127.0.0.1", "test-agent", "https://ref.com");

        assertEquals("https://example.com", result);
        verify(clickEventRepository).save(any());
    }

    @Test
    void createShortUrl_WithPassword_ShouldHashPassword() {
        when(base62Util.generateShortCode()).thenReturn("abc1234");
        when(shortUrlRepository.save(any(ShortUrl.class))).thenAnswer(invocation -> {
            ShortUrl su = invocation.getArgument(0);
            su.setId(1L);
            return su;
        });

        ShortUrlRequest request = ShortUrlRequest.builder()
                .url("https://example.com")
                .password("mySecret@123")
                .build();

        ShortUrlResponse response = urlService.createShortUrl(request, testUser);

        assertTrue(response.isPasswordProtected());
        verify(shortUrlRepository).save(argThat(su ->
                su.getPassword() != null && passwordEncoder.matches("mySecret@123", su.getPassword())));
    }

    @Test
    void deleteShortUrl_NotOwnedByUser_ShouldThrow() {
        User otherUser = User.builder().id(99L).build();
        ShortUrl shortUrl = ShortUrl.builder()
                .id(1L)
                .user(otherUser)
                .build();

        when(shortUrlRepository.findById(1L)).thenReturn(Optional.of(shortUrl));

        assertThrows(ResourceNotFoundException.class,
                () -> urlService.deleteShortUrl(1L, testUser));
    }
}
