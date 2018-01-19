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

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Date;


public class FeatureNoBindIpToLocalhostTest {

    private static MongodStarter mongodStarter = MongodStarter.getDefaultInstance();
    private static IMongodConfig mongodConfig = createMongoConfig();

    private MongodExecutable mongodExecutable;
    private MongodProcess mongodProcess;


    @Before
    public void setUp() {
        mongodExecutable = mongodStarter.prepare(mongodConfig);

        try {
            mongodProcess = mongodExecutable.start();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

    }

    @After
    public void tearDown() {
        if (mongodExecutable != null && mongodProcess != null && mongodProcess.isProcessRunning()) {
            mongodProcess.stop();
            mongodExecutable.stop();
        }
    }

    @Test
    public void testInsert() {
        MongoClient mongo = new MongoClient();
        DB db = mongo.getDB("test");
        DBCollection col = db.createCollection("testCol", new BasicDBObject());
        col.save(new BasicDBObject("testDoc", new Date()));
    }

    private static IMongodConfig createMongoConfig() {
        try {
            return new MongodConfigBuilder()
                    .version(Version.Main.V3_6)
                    .net(new Net("localhost",
                            27017,
                            Network.localhostIsIPv6()))
                    .build();
        } catch (IOException ex) {
            throw new RuntimeException(ex);

        }
    }


}
