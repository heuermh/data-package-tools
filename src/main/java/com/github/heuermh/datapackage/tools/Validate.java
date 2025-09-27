/*
 * The authors of this file license it to you under the
 * Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You
 * may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.github.heuermh.datapackage.tools;

import java.nio.file.Path;

import java.util.Arrays;
import java.util.List;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * Data package tools validate command.
 *
 * @author  Michael Heuer
 */
@Command(name = "validate")
public final class Validate implements Callable<Integer> {

    @Option(names = { "--verbose" })
    private boolean verbose;

    @Parameters
    private List<Path> inputPaths;

    /** Logger. */
    private final Logger logger = LoggerFactory.getLogger(Validate.class);

    @Override
    public Integer call() throws Exception {

        if (inputPaths == null || inputPaths.isEmpty()) {
            logger.info("Reading from <stdin>");
        }
        else {
            for (Path inputPath : inputPaths) {
                logger.info("Reading from path {}", (inputPath == null || "-".equals(inputPath.toString())) ? "<stdin>" : inputPath);
            }
        }

        return 0;
    }


    /**
     * Main.
     *
     * @param args command line args
     */
    public static void main(final String[] args) {

        // cheat to set system property before initializing logger
        if (Arrays.asList(args).contains("--verbose")) {
            System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "info");
        }

        // install a signal handler to exit on SIGPIPE
        sun.misc.Signal.handle(new sun.misc.Signal("PIPE"), new sun.misc.SignalHandler() {
                @Override
                public void handle(final sun.misc.Signal signal) {
                    System.exit(0);
                }
            });

        System.exit(new CommandLine(new Validate()).execute(args));
    }
}
