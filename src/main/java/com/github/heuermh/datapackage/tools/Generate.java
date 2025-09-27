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

import static org.dishevelled.compress.Readers.reader;
import static org.dishevelled.compress.Writers.writer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

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
 * Data package tools generate command.
 *
 * @author  Michael Heuer
 */
@Command(name = "generate")
public final class Generate implements Callable<Integer> {

    @Option(names = { "--verbose" })
    private boolean verbose;

    @Option(names = { "-a", "--agent" }, required = true)
    private String agent;

    @Option(names = { "-p", "--prefix" }, required = true)
    private String prefix;

    @Option(names = { "-s", "--suffix" })
    private String suffix;

    @Option(names = { "-o", "--output-path" })
    private Path outputPath;

    @Parameters
    private List<Path> inputPaths;

    /** Logger. */
    private final Logger logger = LoggerFactory.getLogger(Generate.class);

    @Override
    public Integer call() throws Exception {

        if (inputPaths == null || inputPaths.isEmpty()) {
            try (BufferedReader reader = reader(null)) {
                logger.info("Reading from <stdin>");
                while (reader.ready()) {
                    String line = reader.readLine();
                }
            }
            catch (IOException e) {
                logger.error("Unable to read from <stdin>");
                throw e;
            }
        }
        else {
            for (Path inputPath : inputPaths) {
                try (BufferedReader reader = reader(inputPath)) {
                    logger.info("Reading from path {}", (inputPath == null || "-".equals(inputPath.toString())) ? "<stdin>" : inputPath);
                    while (reader.ready()) {
                        String line = reader.readLine();
                    }
                }
                catch (IOException e) {
                    logger.error("Unable to read from {}", (inputPath == null || "-".equals(inputPath.toString())) ? "<stdin>" : inputPath);
                    throw e;
                }
            }
        }

        logger.info("Writing to {}", outputPath == null ? "<stdout>" : "output path " + outputPath);
        try (PrintWriter writer = writer(outputPath)) {
            writer.println("");
        }
        catch (IOException e) {
            logger.error("Unable to write to {}", outputPath == null ? "<stdout>" : "output path " + outputPath);
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

        System.exit(new CommandLine(new Generate()).execute(args));
    }
}
