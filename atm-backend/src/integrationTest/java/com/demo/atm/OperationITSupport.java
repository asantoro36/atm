package com.demo.atm;

import com.demo.atm.application.port.out.AccountRepository;
import com.demo.atm.application.port.out.AuditLog;
import com.demo.atm.application.port.out.AuditWriteException;
import com.demo.atm.application.port.out.CardRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(OutputCaptureExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
abstract class OperationITSupport {

    protected static final String KEY = "3f2b8c1e-6a4d-4e7b-9c15-8d2f0a7e4b61";
    protected static final String OTHER_KEY = "7d1e4a90-2c3b-4f56-8a71-1b9c0d3e5f22";
    protected static final String CARD = "1111111111111111";
    protected static final String INACTIVE_CARD = "2222222222222222";
    protected static final String UNKNOWN_CARD = "3333333333333333";

    private static final String TRANSACTION_ID_FIELD = "\"transactionId\":\"";

    @TempDir
    private static Path directory;

    protected final ExecutorService executor = Executors.newFixedThreadPool(2);

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @MockitoSpyBean
    protected CardRepository cardRepository;

    @MockitoSpyBean
    protected AuditLog auditLog;

    @LocalServerPort
    private int port;

    @Autowired
    private AccountRepository accountRepository;

    @DynamicPropertySource
    static void auditFile(DynamicPropertyRegistry registry) {
        registry.add("atm.audit.file", () -> auditFilePath().toString());
    }

    @BeforeEach
    void cleanAuditFile() throws IOException {
        Files.deleteIfExists(auditFilePath());
    }

    @AfterEach
    void stopExecutor() {
        executor.shutdownNow();
    }

    protected static Path auditFilePath() {
        return directory.resolve("transactions.log");
    }

    protected List<String> auditLines() throws IOException {
        return Files.readAllLines(auditFilePath());
    }

    protected BigDecimal balanceOf(String account) {
        return accountRepository.findByNumber(account).orElseThrow().balance();
    }

    protected String transactionIdOf(String json) {
        int start = json.indexOf(TRANSACTION_ID_FIELD) + TRANSACTION_ID_FIELD.length();
        return json.substring(start, json.indexOf('"', start));
    }

    protected AuditWriteException auditFailure() {
        return new AuditWriteException(new IOException("disk failure"));
    }

    protected HttpResponse<String> result(Future<HttpResponse<String>> future) throws Exception {
        return future.get(10, TimeUnit.SECONDS);
    }

    protected HttpResponse<String> postTo(String path, String key, String body)
            throws IOException, InterruptedException {
        HttpRequest request = requestBuilder(path)
                .header("Idempotency-Key", key)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    protected HttpResponse<String> postWithoutKey(String path, String body) throws IOException, InterruptedException {
        HttpRequest request = requestBuilder(path)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpRequest.Builder requestBuilder(String path) {
        return HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json");
    }
}
