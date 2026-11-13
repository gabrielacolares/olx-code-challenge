package br.com.olx.anuncios.integration.moderation.http;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.olx.anuncios.config.RateLimiterConfiguration;
import br.com.olx.anuncios.integration.moderation.resilience.ModerationCooldown;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

class ModerationRequestInterceptorTest {
    @Test
    void readsRetryAfterFrom429AndStartsCooldownForSubsequentRequests() throws Exception {
        ModerationCooldown cooldown = mock(ModerationCooldown.class);
        HttpRequest request = mock(HttpRequest.class);
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        ClientHttpResponse response = mock(ClientHttpResponse.class);
        byte[] body = new byte[0];
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.RETRY_AFTER, "2");

        when(execution.execute(request, body)).thenReturn(response);
        when(response.getStatusCode()).thenReturn(HttpStatus.TOO_MANY_REQUESTS);
        when(response.getHeaders()).thenReturn(headers);
        when(cooldown.pauseFrom(headers)).thenReturn(Duration.ofSeconds(2));

        ModerationRequestInterceptor interceptor =
                new ModerationRequestInterceptor(cooldown, new RateLimiterConfiguration().moderationRateLimiter());

        assertSame(response, interceptor.intercept(request, body, execution));

        verify(cooldown, times(2)).awaitAllowed();
        verify(cooldown).pauseFrom(headers);
    }

    @Test
    void limitsSixFastDispatchesToLessThanFivePerSecond() throws Exception {
        ModerationCooldown cooldown = mock(ModerationCooldown.class);
        HttpRequest request = mock(HttpRequest.class);
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        ClientHttpResponse response = mock(ClientHttpResponse.class);
        byte[] body = new byte[0];

        when(execution.execute(request, body)).thenReturn(response);
        when(response.getStatusCode()).thenReturn(HttpStatus.OK);

        ModerationRequestInterceptor interceptor =
                new ModerationRequestInterceptor(cooldown, new RateLimiterConfiguration().moderationRateLimiter());

        long startedAt = System.nanoTime();
        for (int i = 0; i < 6; i++) {
            interceptor.intercept(request, body, execution);
        }
        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);

        assertTrue(elapsedMillis >= 1_000, "Six dispatches must span at least one second");
        verify(execution, times(6)).execute(request, body);
    }

    @Test
    void doesNotStartCooldownForNon429Responses() throws Exception {
        ModerationCooldown cooldown = mock(ModerationCooldown.class);
        HttpRequest request = mock(HttpRequest.class);
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        ClientHttpResponse response = mock(ClientHttpResponse.class);
        byte[] body = new byte[0];

        when(execution.execute(request, body)).thenReturn(response);
        when(response.getStatusCode()).thenReturn(HttpStatus.OK);

        ModerationRequestInterceptor interceptor =
                new ModerationRequestInterceptor(cooldown, new RateLimiterConfiguration().moderationRateLimiter());

        assertSame(response, interceptor.intercept(request, body, execution));

        verify(cooldown, times(2)).awaitAllowed();
        verify(cooldown, never()).pauseFrom(org.mockito.ArgumentMatchers.any());
    }
}
