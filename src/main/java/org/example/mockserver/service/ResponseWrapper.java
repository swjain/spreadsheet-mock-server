package org.example.mockserver.service;

public class ResponseWrapper {
    Boolean isAPIRolledOut;
    String upstreamName;
    MockResponse mockResponse;

    public ResponseWrapper(Boolean isAPIRolledOut, String upstreamName, MockResponse mockResponse) {
        this.isAPIRolledOut = isAPIRolledOut;
        this.upstreamName = upstreamName;
        this.mockResponse = mockResponse;
    }

    public Boolean isAPIRolledOut() {
        return isAPIRolledOut;
    }

    public String getUpstreamName() {
        return upstreamName;
    }

    public MockResponse getMockResponse() {
        return mockResponse;
    }
}
