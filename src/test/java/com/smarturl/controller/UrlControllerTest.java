package com.smarturl.controller;

import com.smarturl.dto.ShortUrlRequest;
import com.smarturl.dto.ShortUrlResponse;
import com.smarturl.entity.User;
import com.smarturl.service.UrlService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UrlControllerTest {

    @Mock
    private UrlService urlService;

    @InjectMocks
    private UrlController urlController;

    @Test
    void createShortUrl_ShouldReturnCreated() {
        User user = User.builder().id(1L).username("testuser").build();
        ShortUrlRequest request = ShortUrlRequest.builder().url("https://example.com").build();
        ShortUrlResponse response = ShortUrlResponse.builder()
                .id(1L).shortCode("abc").longUrl("https://example.com")
                .shortUrl("http://localhost:8080/r/abc").build();

        when(urlService.createShortUrl(any(ShortUrlRequest.class), any(User.class)))
                .thenReturn(response);

        ResponseEntity<?> result = urlController.createShortUrl(request, user);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
    }
}
