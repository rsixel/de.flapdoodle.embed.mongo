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

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.flapdoodle.embed.mongo.config.IMongoConfig;
import de.flapdoodle.embed.mongo.runtime.Mongod;
import de.flapdoodle.embed.process.config.IRuntimeConfig;
import de.flapdoodle.embed.process.config.io.ProcessOutput;
import de.flapdoodle.embed.process.distribution.Distribution;
import de.flapdoodle.embed.process.io.LogWatchStreamProcessor;
import de.flapdoodle.embed.process.io.Processors;
import de.flapdoodle.embed.process.io.StreamToLineProcessor;
import de.flapdoodle.embed.process.runtime.AbstractProcess;
import de.flapdoodle.embed.process.runtime.Executable;
import de.flapdoodle.embed.process.runtime.IStopable;
import de.flapdoodle.embed.process.runtime.ProcessControl;
import java.lang.reflect.Field;
import java.util.logging.Level;

public abstract class AbstractMongoProcess<T extends IMongoConfig, E extends Executable<T, P>, P extends IStopable> extends AbstractProcess<T, E, P> {

    private static Logger logger = LoggerFactory.getLogger(AbstractMongoProcess.class);

    boolean stopped = false;

    public AbstractMongoProcess(Distribution distribution, T config, IRuntimeConfig runtimeConfig, E executable)
            throws IOException {
        super(distribution, config, runtimeConfig, executable);
    }

    @Override
    protected final void onAfterProcessStart(ProcessControl process, IRuntimeConfig runtimeConfig) throws IOException {
        ProcessOutput outputConfig = runtimeConfig.getProcessOutput();
        LogWatchStreamProcessor logWatch = new LogWatchStreamProcessor(successMessage(), knownFailureMessages(),
                StreamToLineProcessor.wrap(outputConfig.getOutput()));
        Processors.connect(process.getReader(), logWatch);
        Processors.connect(process.getError(), StreamToLineProcessor.wrap(outputConfig.getError()));
        logWatch.waitForResult(getConfig().timeout().getStartupTimeout());
        if (logWatch.isInitWithSuccess()) {
            setProcessId(Mongod.getMongodProcessId(logWatch.getOutput(), -1));
        } else {
            String failureFound = logWatch.getFailureFound();
            if (failureFound == null) {
                failureFound = "\n"
                        + "----------------------\n"
                        + "Hmm.. no failure message.. \n"
                        + "...the cause must be somewhere in the process output\n"
                        + "----------------------\n"
                        + "" + logWatch.getOutput();
            }
            try {
                // Process could be finished with success here! In this case no need to throw an exception!
                if (process.waitFor() != 0) {
                    throw new IOException("Could not start process: " + failureFound);
                }
            } catch (InterruptedException e) {
                throw new IOException("Could not start process: " + failureFound, e);
            }
        }
    }

    protected String successMessage() {
        String result = "waiting for connections on port";

        if (getDistribution() != null) {
            String[] v = getDistribution().getVersion().asInDownloadPath().split("\\.");

            int version = Integer.parseInt(v[0]);
            int minor = Integer.parseInt(v[1]);

            if (version == 4 && minor >= 2 || version > 4) {
                result = "Waiting for connections";
            }
        }

        return result;
    }

    private Set<String> knownFailureMessages() {
        HashSet<String> ret = new HashSet<>();
        ret.add("failed errno");
        ret.add("ERROR:");
        ret.add("error command line");
        return ret;
    }

    @Override
    public void stopInternal() {
        synchronized (this) {
            if (!stopped) {

                stopped = true;

                logger.debug("try to stop mongod");
                if (!sendStopToMongoInstance()) {
                    logger.warn("could not stop mongod with db command, try next");
                    if (!sendKillToProcess()) {
                        logger.warn("could not stop mongod, try next");
                        if (!tryKillToProcess()) {
                            logger.warn("could not stop mongod the second time, try one last thing");
                        }
                    }
                }

                stopProcess();
            }
        }
    }

    @Override
    protected void cleanupInternal() {
        deleteTempFiles();
    }

    protected void deleteTempFiles() {

    }

    protected final boolean sendStopToMongoInstance() {
        try {
            return Mongod.sendShutdown(getConfig().net().getServerAddress(), getConfig().net().getPort());
        } catch (UnknownHostException e) {
            logger.error("sendStop", e);
        }
        return false;
    }

    protected Distribution getDistribution() {
        try {
            Field f = AbstractProcess.class.getDeclaredField("distribution");
            f.setAccessible(true);

            return (Distribution) f.get(this);
        } catch (Exception ex) {
            java.util.logging.Logger.getLogger(AbstractMongoProcess.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }

}
