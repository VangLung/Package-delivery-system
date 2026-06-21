package com.example.backend.service;

import java.util.concurrent.atomic.AtomicInteger;

public class ImportJob {
    public volatile String status = "RUNNING";
    public final AtomicInteger imported = new AtomicInteger();
    public final AtomicInteger failed = new AtomicInteger();
}
