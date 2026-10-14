package com.lecture.user.service;

import com.lecture.user.dto.AuthDto;
import com.lecture.user.error.ApiException;
import com.lecture.user.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
@RequiredArgsConstructor
public class OAuthTokenExchangeService {

    @Value("${app.auth.token-url}")
    private String tokenUrl;

    @Value("${app.auth.web-client-id}")
    private String clientId;

    @Value("${app.auth.web-client-secret:}")
    private String clientSecret;

    @Value("${app.auth.redirect-uri}")
    private String redirectUri;

    public AuthDto.OAuthTokenResponse exchangeCode(AuthDto.OAuthCodeExchangeRequest request) {
        if (!StringUtils.hasText(clientSecret)) {
            throw new IllegalStateException("AUTH_WEB_CLIENT_SECRET 환경변수가 필요합니다");
        }

        LinkedMultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("code", request.getCode());
        body.add("redirect_uri", redirectUri);

        try {
            AuthDto.AuthorizationServerTokenResponse response = RestClient.create()
                    .post()
                    .uri(tokenUrl)
                    .headers(headers -> headers.setBasicAuth(clientId, clientSecret))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(AuthDto.AuthorizationServerTokenResponse.class);
            if (response == null || !StringUtils.hasText(response.getAccessToken())) {
                throw new ApiException(ErrorCode.OAUTH_TOKEN_EXCHANGE_FAILED);
            }
            return new AuthDto.OAuthTokenResponse(
                    response.getAccessToken(), response.getTokenType(), response.getExpiresIn()
            );
        } catch (RestClientException exception) {
            throw new ApiException(ErrorCode.OAUTH_TOKEN_EXCHANGE_FAILED);
        }
    }
}
