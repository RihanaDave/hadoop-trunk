/**
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
package org.apache.hadoop.yarn.server.resourcemanager.scheduler.capacity;

import java.io.IOException;
import java.util.List;

import org.apache.hadoop.yarn.server.resourcemanager.placement.csmappingrule.MappingRule;
import org.apache.hadoop.yarn.server.resourcemanager.placement.QueueMapping;
import org.junit.After;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.server.resourcemanager.RMContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestQueueMappings {

  private static final Logger LOG =
      LoggerFactory.getLogger(TestQueueMappings.class);

  private static final String Q1 = "q1";
  private static final String Q2 = "q2";

  private final static String Q1_PATH =
      CapacitySchedulerConfiguration.ROOT + "." + Q1;
  private final static String Q2_PATH =
      CapacitySchedulerConfiguration.ROOT + "." + Q2;
  private static final QueuePath ROOT = new QueuePath(CapacitySchedulerConfiguration.ROOT);
  private static final QueuePath Q1_QUEUE_PATH = new QueuePath(Q1_PATH);
  private static final QueuePath Q2_QUEUE_PATH = new QueuePath(Q2_PATH);

  private CapacityScheduler cs;
  private YarnConfiguration conf;

  @Before
  public void setup() {
    CapacitySchedulerConfiguration csConf =
        new CapacitySchedulerConfiguration();
    setupQueueConfiguration(csConf);
    conf = new YarnConfiguration(csConf);
    cs = new CapacityScheduler();

    RMContext rmContext = TestUtils.getMockRMContext();
    cs.setConf(conf);
    cs.setRMContext(rmContext);
    cs.init(conf);
    cs.start();
  }

  @After
  public void tearDown() {
    if (cs != null) {
      cs.stop();
    }
  }

  private void setupQueueConfiguration(CapacitySchedulerConfiguration conf) {
    // Define top-level queues
    conf.setQueues(ROOT, new String[] {Q1, Q2});

    conf.setCapacity(Q1_QUEUE_PATH, 10);
    conf.setCapacity(Q2_QUEUE_PATH, 90);

    LOG.info("Setup top-level queues q1 and q2");
  }

  @Test
  public void testQueueMappingSpecifyingNotExistedQueue() {
    // if the mapping specifies a queue that does not exist, reinitialize will
    // be failed
    conf.set(CapacitySchedulerConfiguration.QUEUE_MAPPING,
        "u:user:non_existent_queue");
    boolean fail = false;
    try {
      cs.reinitialize(conf, null);
    } catch (IOException ioex) {
      fail = true;
    }
    Assert.assertTrue("queue initialization failed for non-existent q", fail);
  }

  @Test
  public void testQueueMappingTrimSpaces() throws IOException {
    // space trimming
    conf.set(CapacitySchedulerConfiguration.QUEUE_MAPPING, "    u : a : " + Q1);
    cs.reinitialize(conf, null);

    List<MappingRule> rules = cs.getConfiguration().getMappingRules();

    String ruleStr = rules.get(0).toString();
    assert(ruleStr.contains("variable='%user'"));
    assert(ruleStr.contains("value='a'"));
    assert(ruleStr.contains("queueName='q1'"));
  }

  @Test
  public void testQueueMappingPathParsing() {
    QueueMapping leafOnly = QueueMapping.QueueMappingBuilder.create()
        .parsePathString("leaf")
        .build();

    Assert.assertEquals("leaf", leafOnly.getQueue());
    Assert.assertEquals(null, leafOnly.getParentQueue());
    Assert.assertEquals("leaf", leafOnly.getFullPath());

    QueueMapping twoLevels = QueueMapping.QueueMappingBuilder.create()
        .parsePathString("root.leaf")
        .build();

    Assert.assertEquals("leaf", twoLevels.getQueue());
    Assert.assertEquals("root", twoLevels.getParentQueue());
    Assert.assertEquals("root.leaf", twoLevels.getFullPath());

    QueueMapping deep = QueueMapping.QueueMappingBuilder.create()
        .parsePathString("root.a.b.c.d.e.leaf")
        .build();

    Assert.assertEquals("leaf", deep.getQueue());
    Assert.assertEquals("root.a.b.c.d.e", deep.getParentQueue());
    Assert.assertEquals("root.a.b.c.d.e.leaf", deep.getFullPath());
  }

  @Test (timeout = 60000)
  public void testQueueMappingParsingInvalidCases() throws Exception {
    // configuration parsing tests - negative test cases
    checkInvalidQMapping(conf, cs, "x:a:b", "invalid specifier");
    checkInvalidQMapping(conf, cs, "u:a", "no queue specified");
    checkInvalidQMapping(conf, cs, "g:a", "no queue specified");
    checkInvalidQMapping(conf, cs, "u:a:b,g:a",
        "multiple mappings with invalid mapping");
    checkInvalidQMapping(conf, cs, "u:a:b,g:a:d:e", "too many path segments");
    checkInvalidQMapping(conf, cs, "u::", "empty source and queue");
    checkInvalidQMapping(conf, cs, "u:", "missing source missing queue");
    checkInvalidQMapping(conf, cs, "u:a:", "empty source missing q");
  }

  private void checkInvalidQMapping(YarnConfiguration conf,
      CapacityScheduler cs,
      String mapping, String reason)
      throws IOException {
    boolean fail = false;
    try {
      conf.set(CapacitySchedulerConfiguration.QUEUE_MAPPING, mapping);
      cs.reinitialize(conf, null);
    } catch (IOException ex) {
      fail = true;
    }
    Assert.assertTrue("invalid mapping did not throw exception for " + reason,
        fail);
  }
}
