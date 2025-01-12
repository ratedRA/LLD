package com.ratedra.thread;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

interface Transaction {
    void createOrUpdate();

    void commit();

    void rollback();
}

class User implements Transaction {
    @Override
    public void createOrUpdate() {
        System.out.println("user successFully created");
    }

    @Override
    public void commit() {
        System.out.println("user update committed successfully");
    }

    @Override
    public void rollback() {
        System.out.println("user updated rolled-back");
    }
}

class Order implements Transaction {
    @Override
    public void createOrUpdate() {
        System.out.println("order successFully created");
    }

    @Override
    public void commit() {
        System.out.println("order update committed successfully");
    }

    @Override
    public void rollback() {
        System.out.println("order updated rolled-back");
    }
}

class Notification implements Transaction {
    @Override
    public void createOrUpdate() {
        System.out.println("Notification successFully created");
    }

    @Override
    public void commit() {
        System.out.println("Notification update committed successfully");
    }

    @Override
    public void rollback() {
        System.out.println("Notification updated rolled-back");
    }
}

class ErrorService implements Transaction {
    @Override
    public void createOrUpdate() {
        throw new RuntimeException("failed to create or update");
    }

    @Override
    public void commit() {
        System.out.println("ErrorService update committed successfully");
    }

    @Override
    public void rollback() {
        System.out.println("ErrorService updated rolled-back");
    }
}

class Orchestrator {
    public void orchestrateDistributedTransaction(boolean forceError) {
        ExecutorService executorService = Executors.newFixedThreadPool(4);
        User user = new User();
        Order order = new Order();
        Notification notification = new Notification();
        ErrorService errorService = new ErrorService();
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        futures.add(CompletableFuture.runAsync(() -> user.createOrUpdate(), executorService));
        futures.add(CompletableFuture.runAsync(() -> order.createOrUpdate(), executorService));
        futures.add(CompletableFuture.runAsync(() -> notification.createOrUpdate(), executorService));
        if (forceError) {
            futures.add(CompletableFuture.runAsync(() -> errorService.createOrUpdate(), executorService));
        }
        List<CompletableFuture<Void>> wrappedFutures = new ArrayList<>();
        AtomicBoolean failed = new AtomicBoolean(false);

        futures.forEach(future -> {
            wrappedFutures.add(future.handle((result, ex) -> {
                if (ex != null) {
                    System.out.println("Transaction failed due to: " + ex.getMessage());
                    failed.set(true);
                }
                return null;
            }));
        });
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(wrappedFutures.toArray(new CompletableFuture[0]));

        if (failed.get()) {
            handleError(forceError, user, executorService, order, notification, errorService);
        } else {
            handleSuccess(forceError, user, executorService, order, notification, errorService);
        }
        executorService.shutdown();
    }

    private static void handleSuccess(boolean forceError, User user, ExecutorService executorService, Order order, Notification notification, ErrorService errorService) {
        System.out.println("update success, commiting");
        List<CompletableFuture<Void>> commitFutures = new ArrayList<>();
        commitFutures.add(CompletableFuture.runAsync(() -> user.commit(), executorService));
        commitFutures.add(CompletableFuture.runAsync(() -> order.commit(), executorService));
        commitFutures.add(CompletableFuture.runAsync(() -> notification.commit(), executorService));
        if (forceError) {
            commitFutures.add(CompletableFuture.runAsync(() -> errorService.commit(), executorService));
        }

        CompletableFuture<Void> allOf = CompletableFuture.allOf(commitFutures.toArray(new CompletableFuture[0]));
        allOf.thenRun(() -> commitFutures.forEach(r -> {
            r.exceptionally(ex -> {
                System.out.println("failed to commit");
                return null;
            }).thenRun(() -> System.out.println("commit finished successfully"));
        }));
    }

    private static void handleError(boolean forceError, User user, ExecutorService executorService, Order order, Notification notification, ErrorService errorService) {
        System.out.println("update failed, rolling back");
        List<CompletableFuture<Void>> rollbackFuture = new ArrayList<>();
        rollbackFuture.add(CompletableFuture.runAsync(() -> user.rollback(), executorService));
        rollbackFuture.add(CompletableFuture.runAsync(() -> order.rollback(), executorService));
        rollbackFuture.add(CompletableFuture.runAsync(() -> notification.rollback(), executorService));
        if (forceError) {
            rollbackFuture.add(CompletableFuture.runAsync(() -> errorService.rollback(), executorService));
        }

        CompletableFuture<Void> allOf = CompletableFuture.allOf(rollbackFuture.toArray(new CompletableFuture[0]));
        allOf.thenRun(() -> rollbackFuture.forEach(r -> {
            r.exceptionally(ex -> {
                System.out.println("failed to rollback transactions");
                return null;
            }).thenRun(() -> System.out.println("rollback finished succesfully"));
        }));
    }
}


public class SagaPattern {
    public static void main(String[] args) {
        Orchestrator orchestrator = new Orchestrator();
        orchestrator.orchestrateDistributedTransaction(true);
        orchestrator.orchestrateDistributedTransaction(false);
    }
}
