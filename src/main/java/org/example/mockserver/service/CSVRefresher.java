package org.example.mockserver.service;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVWriter;
import com.opencsv.RFC4180Parser;
import com.opencsv.RFC4180ParserBuilder;
import com.opencsv.exceptions.CsvValidationException;

import jakarta.annotation.PostConstruct;

@Component
@Configuration
public class CSVRefresher {
    private Logger logger = LoggerFactory.getLogger("MockServerController");

    @Autowired MockResponseService service;

    @Value("${gsheet_application_name}") private String applicationName;
    @Value("${gsheet_client_file_path}") private String clientFilePath;
    @Value("${gsheet_spreadsheet_id}") private String spreadsheetId;
    @Value("${gsheet_range}") private String range;
    @Value("${gsheet_csv_gen_path}") private String gsheet_csv_gen_path;
    @Value("${gsheet_csv_gen_path}") private String csvInput;

    @PostConstruct
    public void init() throws IOException, CsvValidationException, GeneralSecurityException {
        refreshMockResponsesCSV();
        service.refreshMocks(load());
    }

    @Scheduled(cron = "0 */5 * * * *") // Runs every hour at the top of the hour 
    public void scheduledRefresh() throws IOException, CsvValidationException, GeneralSecurityException {
        refreshMockResponsesCSV();
        service.refreshMocks(load());
    }

    Map<String, _record> load() throws IOException, CsvValidationException {
        Map<String, _record> allRecords = new HashMap<>();

        logger.info("application_name: " + applicationName);

        // Pattern reqRegex = Pattern.compile("curl\\s+-X\\s+(\\S+)\\s+\\\"\\S*?((?:[\\/][a-zA-Z0-9]+)+)");
        Pattern methodRegex = Pattern.compile("curl.*?-X\\s+(GET|POST|PUT|PATCH|DELETE)");
        // Pattern pathRegex = Pattern.compile("curl.*?\\\".*?(([\\/][a-zA-Z0-9_-]+)+)(\\\"|[?])");
        Pattern pathRegex = Pattern.compile("curl.*?\\\"(?:[\\$]\\w+)?((?:[\\<\\/][\\w\\-\\<\\>]+)+)(?:\\\"|[?])");
        Pattern caseRegex = Pattern.compile("-H[\\s'\"]+case: (\\S+?)[\\s'\"]");
        Pattern respRegex = Pattern.compile("#\\s*status:\\s*(\\d+)(?:\\n([\\s\\S]*))?");

        RFC4180Parser rfc4180Parser = new RFC4180ParserBuilder().build();
        CSVReaderBuilder csvReaderBuilder = new CSVReaderBuilder(new FileReader(csvInput));
        CSVReader reader = csvReaderBuilder.withCSVParser(rfc4180Parser).build();
    
        // Skip the header
        reader.skip(1);
        
        String[] line;
        while ((line = reader.readNext()) != null) {
            if (line.length < 5) {
                System.out.println("Skipping line, not enough data: " + line[0]);
                continue;
            }

            String api = line[0];
            String isApiRolledOut = line[1];
            String upstreamName = line[2];
            String request = line[3];
            String response = line[4];

            if (!request.startsWith("curl")) {
                // Skip section title or placeholder rows, e.g. "# Pre-estimate"
                // All valid requests start with "curl"
                System.out.println("Skipping line: " + api);
                continue;
            }

            Matcher m = methodRegex.matcher(request);
            if (!m.find()) {
                throw new RuntimeException("Failed to load data from CSV file. Method regex not matched for use-case: " + api);
            }

            String method = m.group(1);

            m = pathRegex.matcher(request);
            if (!m.find()) {
                throw new RuntimeException("Failed to load data from CSV file. Path regex not matched for use-case: " + api);
            }
            String path = m.group(1);

            String _case = "default";
            m = caseRegex.matcher(request);
            if (m.find()) {
                _case = m.group(1);
            }

            m = respRegex.matcher(response);
            Integer status = 0;
            String respBody = "";
            if (m.find()) {
                status = Integer.parseInt(m.group(1));
                respBody = m.group(2);
            }
            _record rec = new _record(method, path, _case, status, respBody, isApiRolledOut, upstreamName);
            allRecords.put(rec.getKey(), rec);
        }
       
        System.out.println("Registered routes: ");
        allRecords.values().forEach(x -> System.out.println(x.getKey()));
        System.out.println("Total: "+  allRecords.size());

        return allRecords;
    }

    private Sheets getSheetsService() throws IOException, GeneralSecurityException {
        FileInputStream serviceAccountStream = new FileInputStream(clientFilePath);

        GoogleCredential credential = GoogleCredential.fromStream(serviceAccountStream)
                .createScoped(List.of("https://www.googleapis.com/auth/spreadsheets.readonly"));

        return new Sheets.Builder(GoogleNetHttpTransport.newTrustedTransport(), JacksonFactory.getDefaultInstance(), credential)
                .setApplicationName(applicationName)
                .build();
    }

    public void refreshMockResponsesCSV() throws IOException, GeneralSecurityException {
        logger.info("Refreshing CSV");

        Sheets service = getSheetsService();
        ValueRange response = service.spreadsheets().values()
                .get(spreadsheetId, range)
                .execute();

        List<List<Object>> values = response.getValues();
        if (values == null || values.isEmpty()) {
            System.out.println("No data found.");
        } else {
            try (CSVWriter writer = new CSVWriter(new FileWriter(gsheet_csv_gen_path))) {
                for (List<Object> row : values) {
                    String[] rowArray = row.stream()
                            .map(Object::toString)
                            .toArray(String[]::new);
                    writer.writeNext(rowArray);
                }
            }
        }
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getClientFilePath() {
        return clientFilePath;
    }

    public void setClientFilePath(String clientFilePath) {
        this.clientFilePath = clientFilePath;
    }

    public String getSpreadsheetId() {
        return spreadsheetId;
    }

    public void setSpreadsheetId(String spreadsheetId) {
        this.spreadsheetId = spreadsheetId;
    }

    public String getRange() {
        return range;
    }

    public void setRange(String range) {
        this.range = range;
    }

    public String getGsheetCSVGenPath() {
        return gsheet_csv_gen_path;
    }

    public void setGsheetCSVGenPath(String gsheetCSVGenPath) {
        this.gsheet_csv_gen_path = gsheetCSVGenPath;
    }

    public String getCsvInput() {
        return csvInput;
    }

    public void setCsvInput(String csvInput) {
        this.csvInput = csvInput;
    }
}
