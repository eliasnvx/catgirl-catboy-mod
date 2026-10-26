package dev.eliasnvx.catgirl.fabric.gametest;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;

/**
 * Lets the client game test run end with exit code 0. Architectury Transformer's dev runtime leaves non-daemon
 * thread pools behind, so after the tests stop the client the JVM can't exit and vanilla's shutdown watchdog crashes
 * it fifteen seconds later. Once the main thread has finished a clean shutdown this exits right away. Only loaded in
 * the game test runs.
 */
public final class ClientTestExit implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientLifecycleEvents.CLIENT_STOPPING.register(minecraft -> {
            Thread main = Thread.currentThread();
            Thread exit = new Thread(() -> {
                try {
                    main.join();
                } catch (InterruptedException e) {
                    return;
                }
                System.exit(0);
            }, "catgirl-test-exit");
            exit.setDaemon(true);
            exit.start();
        });
    }
}
