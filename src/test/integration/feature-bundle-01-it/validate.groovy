/**
 * Copyright (c) 2012 Reficio (TM) - Reestablish your software! All Rights Reserved.
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

//
// $Id$
//

import aQute.lib.osgi.Jar
import org.reficio.p2.utils.TestUtils as Util;

File featureDir = new File(basedir, 'p2.repo/target/repository/features')
assert featureDir.exists()
assert featureDir.listFiles().size() == 1

String jarName = "test.feature_1.0.0.jar"
assert featureDir.listFiles().first().name == jarName

File pluginDir = new File(basedir, 'p2.repo/target/repository/plugins')
assert pluginDir.exists()
assert pluginDir.listFiles().size() == 1

