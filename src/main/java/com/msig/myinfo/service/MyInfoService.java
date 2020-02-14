package com.msig.myinfo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msig.myinfo.config.RequestResponseLoggingInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.net.ssl.HostnameVerifier;
import java.util.Collections;
import java.util.Map;

@Slf4j
@Service
public class MyInfoService {
    @Value("${myinfo_app.callback_url}")
    private String callbackUrl;
    @Value("${myinfo_app.token_url}")
    private String tokenUrl;
    @Value("${myinfo_app.person_url}")
    private String personUrl;
    @Value("${myinfo_app.client_id}")
    private String clientId;
    @Value("${myinfo_app.client_secret}")
    private String clientSecret;
    @Value("${myinfo_app.attributes}")
    private String attributes;

    public String getToken(String code) throws Exception{
        HostnameVerifier hostnameVerifier = new NoopHostnameVerifier();
        CloseableHttpClient httpClient = HttpClients.custom().setSSLHostnameVerifier(hostnameVerifier).build();
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
        requestFactory.setHttpClient(httpClient);
        BufferingClientHttpRequestFactory bufferingClientHttpRequestFactory = new BufferingClientHttpRequestFactory(requestFactory);

        RestTemplate restTemplate = new RestTemplate(bufferingClientHttpRequestFactory);
        restTemplate.setInterceptors(Collections.singletonList(new RequestResponseLoggingInterceptor()));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setCacheControl(CacheControl.noCache());

        MultiValueMap<String, String> params= new LinkedMultiValueMap<String, String>();
        params.add("grant_type","authorization_code");
        params.add("code",code);
        params.add("redirect_uri",callbackUrl);
        params.add("client_id",clientId);
        params.add("client_secret",clientSecret);

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(tokenUrl, entity, String.class);

        // check response
        if (response.getStatusCode() == HttpStatus.OK) {
            log.info("Request Successful");
            String responseBody = response.getBody();
            Map map = new ObjectMapper().readValue(responseBody, Map.class);
            String token = map.get("access_token").toString();
            log.info("token: {}", token);
            return token;
        } else {
            log.info("Request Failed");
            throw new Exception();
        }
    }

    public String getPerson(String token) throws Exception{
        String sub = getSub(token);
        String url=personUrl+"/"+sub+"/";

        HostnameVerifier hostnameVerifier = new NoopHostnameVerifier();
        CloseableHttpClient httpClient = HttpClients.custom().setSSLHostnameVerifier(hostnameVerifier).build();
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
        requestFactory.setHttpClient(httpClient);
        BufferingClientHttpRequestFactory bufferingClientHttpRequestFactory = new BufferingClientHttpRequestFactory(requestFactory);

        RestTemplate restTemplate = new RestTemplate(bufferingClientHttpRequestFactory);
        restTemplate.setInterceptors(Collections.singletonList(new RequestResponseLoggingInterceptor()));

        HttpHeaders headers = new HttpHeaders();
        headers.setCacheControl(CacheControl.noCache());
        headers.setBearerAuth(token);

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("client_id", clientId)
                .queryParam("attributes", attributes);

        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                entity,
                String.class);

        // check response
        if (response.getStatusCode() == HttpStatus.OK) {
            log.info("Request Successful");
            String person = response.getBody();
            log.info("person: {}", person);
            return person;
        } else {
            log.info("Request Failed");
            throw new Exception();
        }
    }

    private String getSub(String token) throws Exception{
        String decoded = decodeJWT(token);
        Map map = new ObjectMapper().readValue(decoded, Map.class);
        String sub = map.get("sub").toString();
        log.info("sub: {}", sub);
        return sub;
    }

    public String decodeJWT(String jwtToken){
        System.out.println("------------ Decode JWT ------------");
        String[] split_string = jwtToken.split("\\.");
        String base64EncodedHeader = split_string[0];
        String base64EncodedBody = split_string[1];
        String base64EncodedSignature = split_string[2];

        System.out.println("~~~~~~~~~ JWT Header ~~~~~~~");
        Base64 base64Url = new Base64(true);
        String header = new String(base64Url.decode(base64EncodedHeader));
        System.out.println("JWT Header : " + header);


        System.out.println("~~~~~~~~~ JWT Body ~~~~~~~");
        String body = new String(base64Url.decode(base64EncodedBody));
        System.out.println("JWT Body : "+body);
        return body;
    }
}
