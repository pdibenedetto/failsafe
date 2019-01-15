/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */
package net.jodah.failsafe.issues;

import net.jodah.concurrentunit.Waiter;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.concurrent.*;

@Test
public class Issue5Test {
  /**
   * Asserts that a failure is handled as expected by a listener registered via whenFailure.
   */
  public void test() throws Throwable {
    Waiter waiter = new Waiter();
    RetryPolicy<Object> retryPolicy = new RetryPolicy<>().withDelay(Duration.ofMillis(100))
        .withMaxDuration(Duration.ofSeconds(2))
        .withMaxRetries(3)
        .handleResult(null);

    ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    Failsafe.with(retryPolicy).with(executor).onFailure(e -> {
      waiter.assertNull(e.result);
      waiter.assertNull(e.failure);
      waiter.resume();
    }).getAsync(() -> null);

    waiter.await(1000);
  }
}
