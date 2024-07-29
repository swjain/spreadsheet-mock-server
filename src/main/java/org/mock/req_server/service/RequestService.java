package org.mock.req_server.service;

import com.google.gson.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URL;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class RequestService {

    @Autowired
    private RestTemplate restTemplate;


    public String getApiResponse(String method, String path)  {
        String apiUrl = "https://script.google.com/macros/s/AKfycbwT9LDodpeeBcI2V-OIEtJonWwQEhcQtD5OtH_nuKw7q-Za8H-qtii3qcE72epLe1nOWA/exec";
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(apiUrl, String.class);

            HttpStatusCode statusCode = response.getStatusCode();

            if (statusCode.is2xxSuccessful()) {
                JsonObject sheetJson = JsonParser.parseString(Objects.requireNonNull(response.getBody())).getAsJsonObject();

                return getMatchedResponse(method, path, sheetJson.getAsJsonArray("data"));
            }
            else{
                return  "Failed: google sheet api issue";
            }
        }
        catch (Exception e) {
            return "Failed: ".concat(e.getLocalizedMessage());
        }

    }

    private String getMatchedResponse(String method, String path, JsonArray sheetJsonArray ) throws Exception {
        AtomicReference<String> responseString = new AtomicReference<>("Not Found");
            for (JsonElement sheetJson : sheetJsonArray) {
                String request = sheetJson.getAsJsonObject().getAsJsonPrimitive("Request").getAsString();

                String curlMethod = extractHttpMethodFromCurl(request);
                String curlPath = extractPathFromCurl(request);
                assert curlMethod != null;
                if (curlMethod.equalsIgnoreCase(method)) {
                    assert curlPath != null;
                    if (curlPath.equalsIgnoreCase(path)) {
                        String responseStr = sheetJson.getAsJsonObject().getAsJsonPrimitive("Response").getAsString();

                        responseString.set(convertToJsonResponse(responseStr));
                    }
                }
            }
            return responseString.get();
    }

    private String extractPathFromCurl(String curlRequest) throws Exception {
        // Regex to match the URL in the curl command
        String regex = "curl.*\\s+\"([^\"]+)\"";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(curlRequest);

        if (matcher.find()) {
            String urlWithPlaceholder = matcher.group(1).replace("\"", ""); // Remove any additional double quotes
            String placeholder = extractPlaceholder(urlWithPlaceholder);
            return extractPathFromUrl(urlWithPlaceholder, placeholder);
        }else {
                return null;
        }
    }

    private  String extractPlaceholder(String url) {
        // Extract the placeholder (assumes the placeholder starts with a $ and is followed by non-slash characters)
        Pattern placeholderPattern = Pattern.compile("^\\$(\\w+)");
        Matcher placeholderMatcher = placeholderPattern.matcher(url);
        if (placeholderMatcher.find()) {
            return placeholderMatcher.group(0); // Return the whole placeholder including the $
        }
        return "";
    }

    private static String extractPathFromUrl(String url, String placeholder) throws Exception {
        try {
            java.net.URL urlObj = null;
            if (placeholder.isEmpty()) {
                urlObj = new java.net.URL(url);
            }
            else {
                // Remove the placeholder from the beginning of the URL
                String urlWithoutPlaceholder = url.replace(placeholder, "");
                // Extract the path from the remaining URL
                urlObj = new java.net.URL("http://placeholder" + urlWithoutPlaceholder); // Add a placeholder host for parsing

            }return urlObj.getPath();
        } catch (Exception e) {
            throw new Exception("Invalid URL", e);
        }
    }

    private  String extractHttpMethodFromCurl(String curlRequest) throws Exception {
        // Regex to match the HTTP method in the curl command
        String regex = "curl\\s+-X\\s+(\\w+)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(curlRequest);

        if (matcher.find()) {
            return matcher.group(1);
        }
        return  null;
    }

    private String convertToJsonResponse(String responseString) throws Exception {
        // Regex to extract the status code and JSON content
        String regex = "#\\s*status:\\s*(\\d+)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(responseString);

        if (matcher.find()) {
            int statusCode = Integer.parseInt(matcher.group(1));

            // Extract the JSON part
            int jsonStartIndex = responseString.indexOf("{");
            if (jsonStartIndex == -1) {
                throw new Exception("JSON content not found in the response string");
            }
            String jsonString = responseString.substring(jsonStartIndex);

            // Parse the JSON content
            Gson gson = new Gson();
            JsonObject jsonBody = JsonParser.parseString(jsonString).getAsJsonObject();

            // Create the final JSON response
            JsonObject responseJson = new JsonObject();
            responseJson.addProperty("statusCode", statusCode);
            responseJson.add("body", jsonBody);

            return gson.toJson(responseJson);
        } else {
            throw new Exception("Status code not found in response string");
        }
    }

}
