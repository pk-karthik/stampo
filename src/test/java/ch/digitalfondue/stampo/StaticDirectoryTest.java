/**
 * Copyright (C) 2015 digitalfondue (info@digitalfondue.ch)
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
package ch.digitalfondue.stampo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Assert;
import org.junit.Test;

import ch.digitalfondue.stampo.TestUtils.InputOutputDirs;

public class StaticDirectoryTest {

  @Test
  public void testCopyStaticDirectory() throws IOException {
    try (InputOutputDirs iod = TestUtils.get()) {
      Path staticDir = iod.inputDir.resolve("static");
      Files.createDirectories(staticDir);

      Files.createFile(staticDir.resolve("test.txt"));
      Files.createDirectories(staticDir.resolve("css"));
      Files.createFile(staticDir.resolve("css/test.css"));

      
      Assert.assertFalse(Files.exists(iod.outputDir.resolve("test.txt")));
      Assert.assertFalse(Files.exists(iod.outputDir.resolve("css/test.css")));
      
      Stampo stampo = new Stampo(iod.inputDir, iod.outputDir);
      stampo.build();

      Assert.assertTrue(Files.exists(iod.outputDir.resolve("test.txt")));
      Assert.assertTrue(Files.exists(iod.outputDir.resolve("css/test.css")));
    }
  }
}