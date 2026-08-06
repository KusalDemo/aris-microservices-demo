package com.aris.order.service;

import com.aris.common.demo.DemoScenario;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Order-side fault injection before / around payment.
 */
@Component
public class OrderScenarioBehaviour {

    public void beforePersistence(DemoScenario scenario) {
        if (scenario == DemoScenario.ORDER_DOWN) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Order service down (ORDER_DOWN)");
        }
        if (scenario == DemoScenario.ORDER_SLOW) {
            sleep(2000);
        }
        if (scenario == DemoScenario.BUSY_SPIKE) {
            sleep(ThreadLocalRandom.current().nextInt(150, 700));
            if (ThreadLocalRandom.current().nextDouble() < 0.25) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Order busy spike before payment");
            }
        }
    }

    public void assertDbAvailable(DemoScenario scenario) {
        if (scenario == DemoScenario.ORDER_DB_DOWN) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Order DB unavailable (ORDER_DB_DOWN)");
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Order interrupted");
        }
    }
}
