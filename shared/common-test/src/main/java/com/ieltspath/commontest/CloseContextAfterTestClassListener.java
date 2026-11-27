package com.ieltspath.commontest;

import org.springframework.test.annotation.DirtiesContext.HierarchyMode;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;

/**
 * Closes the Spring context when its test class ends, while the containers of the class are still running. Testcontainers
 * stops them right after this callback; a context left in the cache would keep its schedulers, listeners and connection
 * pool running against them, and closing it later can block the test thread until a connection attempt times out.
 */
public class CloseContextAfterTestClassListener extends AbstractTestExecutionListener {

    @Override
    public int getOrder() {
        // After-class callbacks run from the highest order down, so this one runs once the SQL script, transaction and
        // dirties-context listeners are done with the context.
        return DirtiesContextTestExecutionListener.ORDER - 1;
    }

    @Override
    public void afterTestClass(TestContext testContext) {
        testContext.markApplicationContextDirty(HierarchyMode.EXHAUSTIVE);
    }
}
