/**
 * Copyright (C) 2011
 *   Michael Mosmann <michael@mosmann.de>
 *   Martin Jöhren <m.joehren@googlemail.com>
 *
 * with contributions from
 * 	konstantin-ba@github,Archimedes Trajano	(trajano@github)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.flapdoodle.embed.mongo;

import com.mongodb.MongoClient;
import de.flapdoodle.embed.mongo.config.*;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.config.IRuntimeConfig;
import de.flapdoodle.embed.process.runtime.Network;
import org.hamcrest.core.Is;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class MongoDumpExecutableTest {

    private static final Logger logger = LoggerFactory.getLogger(MongoDumpExecutableTest.class.getName());

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();
    private MongodExecutable mongodExe;
    private MongodProcess mongod;
    private Net net;


    @Before
    public void setUp() throws IOException {
        net = new Net(Network.getLocalHost().getHostAddress(),
                Network.getFreeServerPort(),
                Network.localhostIsIPv6());
        IMongodConfig mongodConfig = new MongodConfigBuilder().version(Version.Main.PRODUCTION).net(net).build();

        IRuntimeConfig runtimeConfig = new RuntimeConfigBuilder().defaults(Command.MongoD).build();
        mongodExe = MongodStarter.getInstance(runtimeConfig).prepare(mongodConfig);
        mongod = mongodExe.start();
        MongoClient mongoClient = new MongoClient(net.getServerAddress().getHostName(), net.getPort());
        String dumpDir = Thread.currentThread().getContextClassLoader().getResource("dump").getFile();
        mongoRestoreExecutable(dumpDir).start();

        assertThat(mongoClient.getDatabase("restoredb").getCollection("sample").count(), Is.is(3L));
    }

    @After
    public void after() {
        mongod.stop();
        mongodExe.stop();
    }

    @Test
    public void testStartMongoDump() throws IOException {
        IMongoDumpConfig mongoDumpConfig = new MongoDumpConfigBuilder()
                .version(Version.Main.PRODUCTION)
                .net(net)
                .out(temp.getRoot().getAbsolutePath())
                .build();

        MongoDumpStarter.getDefaultInstance().prepare(mongoDumpConfig).start();
        assertTrue(Arrays.stream(temp.getRoot().listFiles()).anyMatch(f -> "restoredb".equals(f.getName())));
    }

    @Test
    public void testStartMongoDumpToArchive() throws IOException {
        IMongoDumpConfig mongoDumpConfig = new MongoDumpConfigBuilder()
                .version(Version.Main.PRODUCTION)
                .net(net)
                .archive(temp.getRoot().getAbsolutePath())
                .build();
        MongoDumpStarter.getDefaultInstance().prepare(mongoDumpConfig).start();

        assertTrue(Arrays.stream(temp.getRoot().listFiles()).anyMatch(f -> "archive".equals(f.getName())));
    }

    private MongoRestoreExecutable mongoRestoreExecutable(String dumpLocation) throws IOException {
        IMongoRestoreConfig mongoRestoreConfig = new MongoRestoreConfigBuilder()
                .version(Version.Main.PRODUCTION)
                .net(net)
                .dropCollection(true)
                .dir(dumpLocation)
                .build();

        return MongoRestoreStarter.getDefaultInstance().prepare(mongoRestoreConfig);
    }

}
