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

import java.io.BufferedReader;
import java.io.IOException;

import java.nio.file.Path;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import java.util.concurrent.Callable;

import com.fasterxml.uuid.Generators;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.spdx.core.IModelCopyManager;
import org.spdx.core.InvalidSPDXAnalysisException;

import org.spdx.library.ModelCopyManager;
import org.spdx.library.SpdxModelFactory;

import org.spdx.library.model.v3_0_1.SpdxModelClassFactoryV3;

import org.spdx.library.model.v3_0_1.core.Bom;
import org.spdx.library.model.v3_0_1.core.CreationInfo;
import org.spdx.library.model.v3_0_1.core.Element;
import org.spdx.library.model.v3_0_1.core.Hash;
import org.spdx.library.model.v3_0_1.core.HashAlgorithm;
import org.spdx.library.model.v3_0_1.core.Relationship;
import org.spdx.library.model.v3_0_1.core.RelationshipType;

import org.spdx.library.model.v3_0_1.dataset.ConfidentialityLevelType;
import org.spdx.library.model.v3_0_1.dataset.DatasetAvailabilityType;
import org.spdx.library.model.v3_0_1.dataset.DatasetPackage;
import org.spdx.library.model.v3_0_1.dataset.DatasetType;

import org.spdx.library.model.v3_0_1.software.FileKindType;
import org.spdx.library.model.v3_0_1.software.SpdxFile;

import org.spdx.storage.simple.InMemSpdxStore;

import org.spdx.v3jsonldstore.JsonLDStore;

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

    @Option(names = { "--bom-name" }, required = true)
    private String bomName;

    @Option(names = { "--dataset-name" }, required = true)
    private String datasetName;

    @Parameters
    private List<Path> inputPaths;

    /** Logger. */
    private final Logger logger = LoggerFactory.getLogger(Generate.class);

    @Override
    public Integer call() throws Exception {

        SpdxModelFactory.init();
        InMemSpdxStore modelStore = new InMemSpdxStore();
        JsonLDStore jsonLdStore = new JsonLDStore(modelStore, true);
        IModelCopyManager copyManager = new ModelCopyManager();

        CreationInfo creationInfo = SpdxModelClassFactoryV3.createCreationInfo(jsonLdStore, prefix + "agent/" + agent, agent, copyManager);

        Bom bom = creationInfo.createBom(prefix + "bom/" + bomName)
            .setName(bomName)
            .build();

        DatasetPackage dataset = bom.createDatasetPackage(prefix + "dataset/" + bomName)
            .setName(datasetName)
            .addDatasetType(DatasetType.OTHER)
            .setConfidentialityLevel(ConfidentialityLevelType.GREEN)
            .build();

        bom.getRootElements().add(dataset);

        Collection<Element> files = new ArrayList<Element>();
        if (inputPaths == null || inputPaths.isEmpty()) {
            try (BufferedReader reader = reader(null)) {
                logger.info("Reading from <stdin>");
                while (reader.ready()) {
                    files.add(createFile(reader.readLine(), dataset));
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
                    logger.info("Reading from {}", (inputPath == null || "-".equals(inputPath.toString())) ? "<stdin>" : "path " + inputPath);
                    while (reader.ready()) {
                        files.add(createFile(reader.readLine(), dataset));
                    }
                }
                catch (IOException e) {
                    logger.error("Unable to read from {}", (inputPath == null || "-".equals(inputPath.toString())) ? "<stdin>" : "path " + inputPath);
                    throw e;
                }
            }
        }

        Relationship contains = bom.createRelationship(prefix + "contains/" + bomName)
            .setRelationshipType(RelationshipType.CONTAINS)
            .setFrom(dataset)
            .addAllTo(files)
            .build();

        bom.getElements().add(contains);

        logger.info("Writing to <stdout>");
        jsonLdStore.serialize(System.out);

        return 0;
    }

    SpdxFile createFile(final String line, final DatasetPackage dataset) throws IOException {

        List<String> tokens = Splitter.on(CharMatcher.anyOf("\t "))
            .omitEmptyStrings()
            .trimResults()
            .splitToList(line);

        if (tokens.size() < 2) {
            throw new IOException("Unable to parse line " + line + ", expected at least 2 tokens, found " + tokens.size());
        }

        String sha256 = tokens.get(0);
        String fileName = tokens.get(1);
        UUID uuid = Generators.timeBasedEpochGenerator().generate();

        try {
            Hash hash = dataset.createHash(prefix + "hash/" + uuid.toString())
                .setAlgorithm(HashAlgorithm.SHA256)
                .setHashValue(sha256)
                .build();

            SpdxFile file = dataset.createSpdxFile(prefix + "file/" + uuid.toString())
                .setName(fileName)
                .setFileKind(fileName.endsWith("/") ? FileKindType.DIRECTORY : FileKindType.FILE)
                .addVerifiedUsing(hash)
                .build();

            return file;
        }
        catch (InvalidSPDXAnalysisException e) {
            throw new IOException("Could not create file from line " + line, e);
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

        System.exit(new CommandLine(new Generate()).execute(args));
    }
}
