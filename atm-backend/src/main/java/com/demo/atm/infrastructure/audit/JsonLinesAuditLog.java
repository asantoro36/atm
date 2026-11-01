package com.demo.atm.infrastructure.audit;

import com.demo.atm.application.port.out.AuditLog;
import com.demo.atm.application.port.out.AuditWriteException;
import com.demo.atm.domain.AuditEvent;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;

public class JsonLinesAuditLog implements AuditLog {

    private static final String EMPTY = "";

    private final Path file;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    public JsonLinesAuditLog(Path file) {
        this.file = file;
    }

    @Override
    public synchronized void record(AuditEvent event) {
        String line = jsonMapper.writeValueAsString(toFields(event)) + System.lineSeparator();
        try {
            createParentDirectory();
            append(line);
        } catch (IOException exception) {
            throw new AuditWriteException(exception);
        }
    }

    private Map<String, String> toFields(AuditEvent event) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("transactionId", event.transactionId());
        fields.put("dateTime", event.dateTime().toString());
        fields.put("type", event.type());
        fields.put("result", event.result().name());
        fields.put("reason", event.reason() == null ? EMPTY : event.reason().name());
        fields.put("card", event.card());
        fields.put("account", event.account());
        fields.put("amount", event.amount() == null ? EMPTY : event.amount());
        return fields;
    }

    private void createParentDirectory() throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        Files.createDirectories(parent);
    }

    private void append(String line) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(line.getBytes(StandardCharsets.UTF_8));
        try (FileChannel channel = FileChannel.open(file, StandardOpenOption.CREATE, StandardOpenOption.WRITE,
                StandardOpenOption.APPEND)) {
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
            channel.force(true);
        }
    }
}
