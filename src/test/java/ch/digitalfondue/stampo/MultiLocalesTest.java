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

import static java.nio.file.Files.write;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

import ch.digitalfondue.stampo.TestUtils.InputOutputDirs;

public class MultiLocalesTest {


  @Test
  public void testMultipleLocales() throws IOException {
    try (InputOutputDirs iod = TestUtils.get()) {

      createFiles(iod);

      write(iod.inputDir.resolve("configuration.yaml"),
          "locales: ['en', 'de','fr']".getBytes(StandardCharsets.UTF_8));

      Stampo stampo = new Stampo(iod.inputDir, iod.outputDir, Collections.emptyMap());

      stampo.build();

      checkForLocale(iod, "en", " | en-de-fr| .-../de-../fr", " | en-de-fr| index2.html-../de/index2.html-../fr/index2.html");
      checkForLocale(iod, "de", " | en-de-fr| ../en-.-../fr", " | en-de-fr| ../en/index2.html-index2.html-../fr/index2.html");
      checkForLocale(iod, "fr", " | en-de-fr| ../en-../de-.", " | en-de-fr| ../en/index2.html-../de/index2.html-index2.html");
    }
  }



  @Test
  public void testMultipleLocalesWithDefaultLang() throws IOException {
    try (InputOutputDirs iod = TestUtils.get()) {

      createFiles(iod);

      write(iod.inputDir.resolve("configuration.yaml"), ("locales: ['en', 'de','fr']\n"
          + "default-locale: en").getBytes(StandardCharsets.UTF_8));

      Stampo stampo = new Stampo(iod.inputDir, iod.outputDir, Collections.emptyMap());

      stampo.build();

      checkForLocale(iod, "de", " | -de-fr| ..-.-../fr", " | -de-fr| ../index2.html-index2.html-../fr/index2.html");
      checkForLocale(iod, "fr", " | -de-fr| ..-../de-.", " | -de-fr| ../index2.html-../de/index2.html-index2.html");

      Assert.assertTrue(Files.exists(iod.outputDir.resolve("index.html")));
      Assert.assertEquals("<h1>Hello World en</h1>[.] [.] | -de-fr| .-de-fr",
          TestUtils.fileOutputAsString(iod, "index.html"));
      
      Assert.assertTrue(Files.exists(iod.outputDir.resolve("index2.html")));
      Assert.assertEquals("<h1>Hello World en</h1>[.] [.] | -de-fr| index2.html-de/index2.html-fr/index2.html",
          TestUtils.fileOutputAsString(iod, "index2.html"));


      Assert.assertTrue(Files.exists(iod.outputDir.resolve("post/first/index.html")));
      Assert.assertEquals("<h1>First en</h1>[../..] [../..]",
          TestUtils.fileOutputAsString(iod, "post/first/index.html"));

      Assert.assertTrue(Files.exists(iod.outputDir.resolve("post/second/index.html")));
      Assert.assertEquals("<h1>Second en</h1>[../..] [../..]",
          TestUtils.fileOutputAsString(iod, "post/second/index.html"));
    }
  }


  private void createFiles(InputOutputDirs iod) throws IOException {
    write(iod.inputDir.resolve("content/index.html.peb"),
        ("<h1>Hello World {{locale}}</h1>"
       +"[{{relativeRootPath}}] [{{relativeRootPathLocalized}}] "
       +"| {{defaultOrLocale('en')}}-{{defaultOrLocale('de')}}-{{defaultOrLocale('fr')}}"
       +"| {{switchToLocale('en')}}-{{switchToLocale('de')}}-{{switchToLocale('fr')}}").getBytes(StandardCharsets.UTF_8));
    
    write(iod.inputDir.resolve("content/index2.html.ftl"),
        ("---\noverride-use-ugly-url: true\n---\n"
            + "<h1>Hello World ${locale}</h1>"
            + "[${relativeRootPath}] [${relativeRootPathLocalized}] "
            + "| ${defaultOrLocale('en')}-${defaultOrLocale('de')}-${defaultOrLocale('fr')}"
            + "| ${switchToLocale('en')}-${switchToLocale('de')}-${switchToLocale('fr')}").getBytes(StandardCharsets.UTF_8));

    Files.createDirectories(iod.inputDir.resolve("content/post"));

    write(iod.inputDir.resolve("content/post/first.html.peb"),
        "<h1>First {{locale}}</h1>[{{relativeRootPath}}] [{{relativeRootPathLocalized}}]".getBytes(StandardCharsets.UTF_8));

    write(iod.inputDir.resolve("content/post/second.html.peb"),
        "<h1>Second {{locale}}</h1>[{{relativeRootPath}}] [{{relativeRootPathLocalized}}]".getBytes(StandardCharsets.UTF_8));
    
    //this file will generate a third.peb and will only be present for the english locale
    write(iod.inputDir.resolve("content/post/third.en.peb"),
        "<h1>Third {{locale}}</h1>[{{relativeRootPath}}] [{{relativeRootPathLocalized}}]".getBytes(StandardCharsets.UTF_8));
    
    
    write(iod.inputDir.resolve("content/post/fourth.fr.de.peb"),
        "<h1>Fourth {{locale}}</h1>[{{relativeRootPath}}] [{{relativeRootPathLocalized}}]".getBytes(StandardCharsets.UTF_8));
  }

  private void checkForLocale(InputOutputDirs iod, String locale, String pebble, String ftl) throws IOException {
    Assert.assertTrue(Files.exists(iod.outputDir.resolve(locale + "/index.html")));
    Assert.assertEquals("<h1>Hello World " + locale + "</h1>[..] [.]" + pebble,
        TestUtils.fileOutputAsString(iod, locale + "/index.html"));
    
    Assert.assertTrue(Files.exists(iod.outputDir.resolve(locale + "/index2.html")));
    Assert.assertEquals("<h1>Hello World " + locale + "</h1>[..] [.]" + ftl,
        TestUtils.fileOutputAsString(iod, locale + "/index2.html"));

    Assert.assertTrue(Files.exists(iod.outputDir.resolve(locale + "/post/first/index.html")));
    Assert.assertEquals("<h1>First " + locale + "</h1>[../../..] [../..]",
        TestUtils.fileOutputAsString(iod, locale + "/post/first/index.html"));

    Assert.assertTrue(Files.exists(iod.outputDir.resolve(locale + "/post/second/index.html")));
    Assert.assertEquals("<h1>Second " + locale + "</h1>[../../..] [../..]",
        TestUtils.fileOutputAsString(iod, locale + "/post/second/index.html"));
    
    if("en".equals(locale)) {
      Assert.assertTrue(Files.exists(iod.outputDir.resolve(locale + "/post/third")));
      Assert.assertEquals("<h1>Third " + locale + "</h1>[../..] [..]",
          TestUtils.fileOutputAsString(iod, locale + "/post/third"));
      
      Assert.assertFalse(Files.exists(iod.outputDir.resolve(locale + "/post/fourth")));
    } else {
      Assert.assertFalse(Files.exists(iod.outputDir.resolve(locale + "/post/third")));
      
      Assert.assertTrue(Files.exists(iod.outputDir.resolve(locale + "/post/fourth")));
      Assert.assertEquals("<h1>Fourth " + locale + "</h1>[../..] [..]",
          TestUtils.fileOutputAsString(iod, locale + "/post/fourth"));
    }
  }
}
