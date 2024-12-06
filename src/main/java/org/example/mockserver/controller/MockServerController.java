package org.example.mockserver.controller;

import jakarta.servlet.http.HttpServletRequest;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Optional;
import java.util.Enumeration;
import java.util.Map;

import org.example.mockserver.service.MockResponse;
import org.example.mockserver.service.MockResponseService;
import org.example.mockserver.service.ResponseWrapper;
import org.example.mockserver.service.UpstreamProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MockServerController {
    private Logger logger = LoggerFactory.getLogger("MockServerController");

    @Value("${example_domain_verification_query_response}") private String exampleDomainVerificationQueryResponse;

    @Autowired
    private MockResponseService requestService;

    @Autowired
    private UpstreamProperties upstreamProperties;

    @RequestMapping("/list")
    public ResponseEntity<String> listAllRegisterdPaths(HttpServletRequest request) {
        return  ResponseEntity.ok(requestService.listAllRegisteredPaths());
    }
    
    @RequestMapping("/**")
    public ResponseEntity<String> index(HttpServletRequest request) throws IOException {
        Map<String, String> upstreams = upstreamProperties.getUpstreams();

        String method = request.getMethod();
        String path = request.getRequestURI();
        String _case = Optional.ofNullable(request.getHeader("case")).orElse("default");
        logger.info("Received request for method: " + method + " path: " + path + " case: " + _case);

        if (path.equals("/goto-auth/partner/v1/authenticate")) {
            logRequest(request);
        }

        ResponseWrapper respWrapper = requestService.getResponse(method, path, _case);

        if (respWrapper.isAPIRolledOut()) {
            String svcName = respWrapper.getUpstreamName();
            String targetHost = upstreams.get(svcName);
            String targetURL = targetHost + path;
            ActualResponse actualResponse = proxyRequest(request, targetURL);
            logger.info("Serving actual response for method: " + method + " path: " + path + " case: " + _case + ", resp.status: " + actualResponse.status + ", resp.body: " + actualResponse.body);
            return ResponseEntity.status(actualResponse.status).body(actualResponse.body);
        }

        MockResponse mockResponse = respWrapper.getMockResponse();
        logger.info("Serving mock response for method: " + method + " path: " + path + " case: " + _case);
        return ResponseEntity.status(mockResponse.getStatus()).body(mockResponse.getBody());
    }

    @RequestMapping("/*.txt")
    public ResponseEntity<String> exampleDomainVerificationCode(HttpServletRequest request) {
        return  ResponseEntity.ok(exampleDomainVerificationQueryResponse);
    }

    private void logRequest(HttpServletRequest request) throws IOException {
        // Read the body of the request
        StringBuilder stringBuilder = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
        }
        String requestBody = stringBuilder.toString();
        logger.info("Received authenticate request. Body: " + requestBody);
    }

    private ActualResponse proxyRequest(HttpServletRequest request, String targetUrl) throws IOException {
        // Create a connection to the target server
        URL url = new URL(targetUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        // Set the method (GET, POST, etc.)
        connection.setRequestMethod(request.getMethod());

        // Copy headers from the original request
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            connection.setRequestProperty(headerName, request.getHeader(headerName));
        }

        connection.setRequestProperty("X_OWNER_ID", "894131");
        connection.setRequestProperty("X_OWNER_TYPE", "customer");
        connection.setRequestProperty("Go-User-ID", "894131");
        connection.setRequestProperty("Go-User-Type", "customer");

        // If the method is POST or PUT, you need to forward the body
        if (request.getMethod().equalsIgnoreCase("POST") || request.getMethod().equalsIgnoreCase("PUT")) {
            connection.setDoOutput(true);
            // Use ByteArrayOutputStream to dynamically handle input stream
            try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                 InputStream inputStream = request.getInputStream()) {
                
                byte[] buffer = new byte[8192]; // 8KB buffer, can be adjusted as needed
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    byteArrayOutputStream.write(buffer, 0, bytesRead);
                }

                // Write the full content to the connection's output stream
                try (OutputStream os = connection.getOutputStream()) {
                    byteArrayOutputStream.writeTo(os);
                }
            }
        }

        // Get the response from the target server
        int responseCode = connection.getResponseCode();
        logger.info("[remote call] response code: " + responseCode);

        InputStream inputStream;
        if (responseCode >= 400) {
            // For error responses (e.g., 400), use getErrorStream
            inputStream = connection.getErrorStream();
        } else {
            // For success responses (e.g., 200), use getInputStream
            inputStream = connection.getInputStream();
        }

        // Read the response
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line).append("\n");
            }
        }

        return new ActualResponse(responseCode, response.toString());
    }

    class ActualResponse{
        Integer status;
        String body;

        public ActualResponse(Integer status, String body) {
            this.status = status;
            this.body = body;
        }
    }

}
