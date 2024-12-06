package org.example.mockserver.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@Configuration
public class MockResponseService {
    // @Autowired
    Map<String, _record> allRecords;

    @Value("${mock_server_host}") private String mockServerHost;

    void refreshMocks(Map<String, _record> allRecords) {
        this.allRecords = allRecords;
    }
    
    public String listAllRegisteredPaths()  {
        String jsonString = "";
        ObjectMapper objectMapper = new ObjectMapper();
        try {

            List<String> recs = new ArrayList<String>();
            allRecords.values().forEach(r -> {
                if (r.getCase().equals("default")) {
                    recs.add("curl -X " + r.method + " " + mockServerHost + r.path);
                } else {
                    recs.add("curl -X " + r.method + " " + mockServerHost + r.path + " -H 'case: " + r._case + "'");
                }
            });
            
            jsonString = objectMapper.writeValueAsString(recs);
            System.out.println(jsonString);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return jsonString;
    }

    public ResponseWrapper getResponse(String method, String path, String _case)  {
        String key = _record.Key(method, path, _case);
        _record rec= allRecords.get(key);
        if (rec == null) {
            return new ResponseWrapper(false, "", new MockResponse(404, "No mock data found for: " + key));
        }

        System.out.println("record: " + rec);

        return new ResponseWrapper(rec.isApiRolledOut, rec.upstreamName, new MockResponse(rec.respStatus, rec.respBody));
    }
}

class _record{
    String method;
    String path;
    String _case;
    Integer respStatus;
    String respBody;
    Boolean isApiRolledOut;
    String upstreamName;

    public _record(String method, String path, String _case, Integer respStatus, String respBody, String isApiRolledOut, String upstreamName) {
        this.method = method;
        this.path = path;
        this._case = _case;
        this.respStatus = respStatus;
        this.respBody = respBody;
        this.isApiRolledOut = isApiRolledOut.equals("yes");
        this.upstreamName = upstreamName;
    }

    public String getCase() {
        return _case;
    }

    public String getKey() {
        return Key(method, path, _case);
    }

    public static String Key(String method, String path, String _case) {
        return method + "_" + path + "_case_" + _case;
    }

    @Override
    public String toString() {
        return "[method: " + method 
        + ", path: " + path 
        + ", case: " + _case 
        + ", isApiRolledOut: " + isApiRolledOut 
        + ", upstreamName: " + upstreamName 
        + ", respStatus: " + respStatus 
        + ", respBody: " + respBody + "]";
    }
}


