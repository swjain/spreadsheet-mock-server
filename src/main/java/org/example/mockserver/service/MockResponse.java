package org.example.mockserver.service;

public class MockResponse {
    private Integer status;
    private String body;

    public MockResponse(Integer status, String body) {
        this.status = status;
        this.body = body;
    }

    public String getBody() {
        return body;
    }

    public Integer getStatus() {
        return status;
    }
}
