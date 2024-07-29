package org.mock.req_server.controller;

import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import org.mock.req_server.service.RequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReqServerController {

    @Autowired
    private RequestService requestService;

    @RequestMapping("/**")
    public ResponseEntity<String> index(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        return  ResponseEntity.ok(requestService.getApiResponse(method, path));
    }
}
