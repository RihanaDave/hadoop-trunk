/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.fs.s3a.auth.delegation;

import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

import org.apache.hadoop.fs.s3a.CredentialInitializationException;

/**
 * Simple AWS credential provider which counts how often it is invoked.
 */
public class CountInvocationsProvider
    implements AwsCredentialsProvider {

  private static final Logger LOG = LoggerFactory.getLogger(
      CountInvocationsProvider.class);

  public static final String NAME = CountInvocationsProvider.class.getName();

  public static final AtomicLong COUNTER = new AtomicLong(0);

  private final AtomicLong instanceCounter = new AtomicLong(0);

  @Override
  public AwsCredentials resolveCredentials() {
    final long global = COUNTER.incrementAndGet();
    final long local = instanceCounter.incrementAndGet();
    final String msg =
        String.format("counter with global count %d and local count %d", global, local);
    LOG.debug("resolving credentials from {}", msg);
    throw new CredentialInitializationException("no credentials from " + msg);
  }

  public long getInstanceCounter() {
    return instanceCounter.get();
  }

  @Override
  public String toString() {
    return "CountInvocationsProvider{" +
        "instanceCounter=" + instanceCounter.get() +
        "; global counter=" + COUNTER.get() +
        '}';
  }

  public static long getInvocationCount() {
    return COUNTER.get();
  }
}
