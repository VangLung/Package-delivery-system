package com.example.backend.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.backend.db.ShipmentsRepoInterface;
import com.example.backend.db.UsersRepoInterface;
import com.example.backend.models.Shipment;

@Service
public class ImportService {

    private static final int BATCH_SIZE = 500;
    private static final int INSERT_THREADS = 6;

    private final ShipmentsRepoInterface shipmentsRepo;
    private final UsersRepoInterface usersRepo;
    private final Map<String, ImportJob> jobs = new ConcurrentHashMap<>();

    public ImportService(ShipmentsRepoInterface shipmentsRepo, UsersRepoInterface usersRepo) {
        this.shipmentsRepo = shipmentsRepo;
        this.usersRepo = usersRepo;
    }

    public String start(MultipartFile file) throws IOException {
        Path temp = Files.createTempFile("import-", ".csv");
        file.transferTo(temp);

        String jobId = UUID.randomUUID().toString();
        ImportJob job = new ImportJob();
        jobs.put(jobId, job);

        new Thread(() -> run(temp, job)).start();
        return jobId;
    }

    public ImportJob job(String jobId) {
        return jobs.get(jobId);
    }

    private void run(Path file, ImportJob job) {
        Set<String> users = usersRepo.findAllUsernames();
        ExecutorService pool = Executors.newFixedThreadPool(INSERT_THREADS);
        List<Shipment> batch = new ArrayList<>(BATCH_SIZE);

        CSVFormat format = CSVFormat.DEFAULT.builder()
            .setHeader().setSkipHeaderRecord(true).setTrim(true).setIgnoreEmptyLines(true).build();

        try (BufferedReader reader = bomSafeReader(file);
             CSVParser parser = format.parse(reader)) {

            for (CSVRecord rec : parser) {
                Shipment s = toShipment(rec);
                if (s.getTrackingNumber() == null || s.getTrackingNumber().isBlank()
                        || !users.contains(s.getCustomerUsername())) {
                    job.failed.incrementAndGet();
                    continue;
                }
                batch.add(s);
                if (batch.size() == BATCH_SIZE) {
                    dispatch(pool, batch, job);
                    batch = new ArrayList<>(BATCH_SIZE);
                }
            }
            if (!batch.isEmpty()) {
                dispatch(pool, batch, job);
            }
        } catch (IOException e) {
            job.status = "FAILED";
        } finally {
            pool.shutdown();
            try {
                pool.awaitTermination(1, TimeUnit.HOURS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            if (!"FAILED".equals(job.status)) {
                job.status = "DONE";
            }
            try {
                Files.deleteIfExists(file);
            } catch (IOException ignored) {
            }
        }
    }

    private void dispatch(ExecutorService pool, List<Shipment> batch, ImportJob job) {
        List<Shipment> b = batch;
        pool.submit(() -> {
            int ok = shipmentsRepo.insertBatch(b);
            job.imported.addAndGet(ok);
            job.failed.addAndGet(b.size() - ok);
        });
    }

    private Shipment toShipment(CSVRecord rec) {
        Shipment s = new Shipment();
        s.setTrackingNumber(get(rec, "tracking_number"));
        s.setDescription(get(rec, "description"));
        String status = get(rec, "current_status");
        s.setCurrentStatus(status == null || status.isBlank() ? "CREATED" : status);
        s.setCustomerUsername(get(rec, "customer_username"));
        String createdAt = get(rec, "created_at");
        if (createdAt != null && !createdAt.isBlank()) {
            try {
                s.setCreatedAt(Timestamp.valueOf(createdAt));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return s;
    }

    private String get(CSVRecord rec, String col) {
        return rec.isMapped(col) ? rec.get(col) : null;
    }

    /** Opens a UTF-8 reader and skips a leading byte-order mark if present. */
    private BufferedReader bomSafeReader(Path file) throws IOException {
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(Files.newInputStream(file), StandardCharsets.UTF_8));
        reader.mark(1);
        if (reader.read() != 0xFEFF) {
            reader.reset();
        }
        return reader;
    }
}
