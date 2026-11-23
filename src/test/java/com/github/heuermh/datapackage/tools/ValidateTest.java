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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.junit.rules.TemporaryFolder;

import org.spdx.library.SpdxModelFactory;

/**
 * Unit test for Validate.
 *
 * @author  Michael Heuer
 */
public final class ValidateTest {
    private Validate validate;

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    public void setUp() {
        SpdxModelFactory.init();
        validate = new Validate();
    }

    @Test
    public void testConstructor() {
        assertNotNull(validate);
    }

    @Test
    public void testValid() throws Exception {
        assertEquals(0, execute(copyResource("valid.spdx.json")));
    }

    @Test
    public void testMalformedJson() throws Exception {
        assertEquals(1, execute(copyResource("malformed.json")));
    }

    @Test
    public void testMissingRequiredProperty() throws Exception {
        assertEquals(1, execute(copyResource("missing-from.spdx.json")));
    }

    @Test
    public void testMultipleInputPathsAllValid() throws Exception {
        assertEquals(0, execute(copyResource("valid.spdx.json"), copyResource("valid.spdx.json")));
    }

    @Test
    public void testMultipleInputPathsOneInvalid() throws Exception {
        assertEquals(1, execute(copyResource("valid.spdx.json"), copyResource("malformed.json")));
    }

    @Test(expected = IOException.class)
    public void testValidateMissingInputPath() throws Exception {
        validate.validate(temporaryFolder.getRoot().toPath().resolve("does-not-exist.spdx.json"));
    }

    @Test
    public void testValidateInputPath() throws Exception {
        assertTrue(validate.validate(copyResource("valid.spdx.json")));
        assertFalse(validate.validate(copyResource("missing-from.spdx.json")));
    }

    private int execute(final Path... inputPaths) {
        String[] args = new String[inputPaths.length];
        for (int i = 0; i < inputPaths.length; i++) {
            args[i] = inputPaths[i].toString();
        }
        return new CommandLine(validate).execute(args);
    }

    private Path copyResource(final String name) throws IOException {
        Path path = temporaryFolder.newFile().toPath();
        try (InputStream inputStream = getClass().getResourceAsStream(name)) {
            Files.copy(inputStream, path, StandardCopyOption.REPLACE_EXISTING);
        }
        return path;
    }
}
