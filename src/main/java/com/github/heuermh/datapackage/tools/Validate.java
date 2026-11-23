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

import static org.dishevelled.compress.InputStreams.inputStream;

import java.io.IOException;
import java.io.InputStream;

import java.nio.file.Path;

import java.util.Arrays;
import java.util.List;

import java.util.concurrent.Callable;

import com.fasterxml.jackson.core.JsonProcessingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.spdx.core.InvalidSPDXAnalysisException;

import org.spdx.library.SpdxModelFactory;

import org.spdx.library.model.v3_0_1.core.SpdxDocument;

import org.spdx.storage.simple.InMemSpdxStore;

import org.spdx.v3jsonldstore.JsonLDStore;

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
        SpdxModelFactory.init();

        boolean valid = true;
        if (inputPaths == null || inputPaths.isEmpty()) {
            valid = validate(null);
        }
        else {
            for (Path inputPath : inputPaths) {
                valid &= validate(inputPath);
            }
        }
        return valid ? 0 : 1;
    }

    /**
     * Validate the specified input path is valid JSON-LD formatted SPDX 3.0.1.
     *
     * @param inputPath input path to validate, or <code>null</code> or <code>-</code> for <code>stdin</code>
     * @return true if the specified input path is valid
     * @throws IOException if an I/O error occurs
     */
    boolean validate(final Path inputPath) throws IOException {
        boolean stdin = (inputPath == null || "-".equals(inputPath.toString()));
        String source = stdin ? "<stdin>" : "path " + inputPath;
        logger.info("Reading from {}", source);

        // use a new model store for each input, to avoid element ID collisions between inputs
        JsonLDStore jsonLdStore = new JsonLDStore(new InMemSpdxStore());
        try (InputStream inputStream = stdin ? System.in : inputStream(inputPath)) {
            SpdxDocument document = (SpdxDocument) jsonLdStore.deSerialize(inputStream, false);
            List<String> verificationErrors = document.verify();
            if (verificationErrors.isEmpty()) {
                logger.info("Validated {}", source);
                return true;
            }
            for (String verificationError : verificationErrors) {
                logger.error("Invalid {}: {}", source, verificationError);
            }
            return false;
        }
        catch (JsonProcessingException | InvalidSPDXAnalysisException | RuntimeException e) {
            logger.error("Invalid {}: {}", source, e.getMessage());
            return false;
        }
        catch (IOException e) {
            logger.error("Unable to read from {}: {}", source, e.getMessage());
            throw e;
        }
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
