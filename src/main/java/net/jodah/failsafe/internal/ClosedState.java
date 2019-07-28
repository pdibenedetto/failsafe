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
package net.jodah.failsafe.internal;

import net.jodah.failsafe.CircuitBreaker;
import net.jodah.failsafe.CircuitBreaker.State;
import net.jodah.failsafe.ExecutionContext;
import net.jodah.failsafe.ExecutionResult;
import net.jodah.failsafe.internal.util.CircularBitSet;
import net.jodah.failsafe.util.Ratio;

public class ClosedState extends CircuitState {
  private final CircuitBreaker breaker;
  private final CircuitBreakerInternals internals;

  public ClosedState(CircuitBreaker breaker, CircuitBreakerInternals internals) {
    this.breaker = breaker;
    this.internals = internals;
    setFailureThreshold(breaker.getFailureThreshold() != null ? breaker.getFailureThreshold() : ONE_OF_ONE);
  }

  @Override
  public boolean allowsExecution() {
    return true;
  }

  @Override
  public State getInternals() {
    return State.CLOSED;
  }

  @Override
  public synchronized void recordFailure(ExecutionResult result, ExecutionContext context) {
    bitSet.setNext(false);
    checkThreshold(result, context);
  }

  @Override
  public synchronized void recordSuccess() {
    bitSet.setNext(true);
    checkThreshold(null, null);
  }

  @Override
  public void setFailureThreshold(Ratio threshold) {
    bitSet = new CircularBitSet(threshold.getDenominator(), bitSet);
  }

  /**
   * Checks to determine if a threshold has been met and the circuit should be opened or closed.
   *
   * <p>
   * When a failure ratio is configured, the circuit is opened after the expected number of executions based on whether
   * the ratio was exceeded.
   * <p>
   * If a failure threshold is configured, the circuit is opened if the expected number of executions fails else it's
   * closed if a single execution succeeds.
   */
  synchronized void checkThreshold(ExecutionResult result, ExecutionContext context) {
    Ratio failureRatio = breaker.getFailureThreshold();

    // Handle failure threshold ratio
    if (failureRatio != null && bitSet.occupiedBits() >= failureRatio.getDenominator()
        && bitSet.negativeRatioValue() >= failureRatio.getValue())
      internals.open(result, context);

    // Handle no thresholds configured
    else if (failureRatio == null && bitSet.negativeRatioValue() == 1)
      internals.open(result, context);
  }
}