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
package ch.digitalfondue.stampo.renderer.freemarker;

import static java.nio.file.Files.newBufferedReader;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import ch.digitalfondue.stampo.renderer.Renderer;
import ch.digitalfondue.stampo.resource.Directory;
import freemarker.cache.TemplateLoader;

class FreemarkerTemplateLoader implements TemplateLoader {


  private final Directory root;
  private final Path contentDir;
  private final Path baseDir;

  public FreemarkerTemplateLoader(Path contentDir, Path baseDir, Directory root) {
    this.contentDir = contentDir;
    this.baseDir = baseDir;
    this.root = root;
  }

  @Override
  public Reader getReader(Object templateSource, String encoding) throws IOException {
    Path template = (Path) templateSource;
    if (template.startsWith(contentDir)) {// content
      return new StringReader(Renderer.getContentFileResource(template, contentDir, root)
          .getContent().orElseThrow(IllegalArgumentException::new));
    } else {// layout or others (import/include)
      return newBufferedReader(template, StandardCharsets.UTF_8);
    }
  }

  @Override
  public long getLastModified(Object templateSource) {
    return -1;
  }

  @Override
  public Object findTemplateSource(String name) throws IOException {
    //handle relative or absolute path names
    Path orig = contentDir.getFileSystem().getPath(name).normalize();
    Path p = contentDir.getFileSystem().getPath("/" + name).normalize();
    if (p.startsWith(baseDir)) {
      return p;
    } else if (orig.startsWith(baseDir)) {
      return orig;
    } else {
      return baseDir.resolve(name);
    }
  }

  @Override
  public void closeTemplateSource(Object templateSource) throws IOException {}
}
