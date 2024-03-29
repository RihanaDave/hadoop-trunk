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

/**
 * Was the location for support for S3 Select.
 * Now removed apart from some constants.f
 * There was a class {@code BlockingEnumeration} which
 * mapped SdkPublisher to an Enumeration.
 * This may be of use elsewhere; it can be retrieved from
 * hadoop commit 8bf72346a59c.
 */
@InterfaceAudience.Private
@InterfaceStability.Unstable
package org.apache.hadoop.fs.s3a.select;

import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.classification.InterfaceStability;
