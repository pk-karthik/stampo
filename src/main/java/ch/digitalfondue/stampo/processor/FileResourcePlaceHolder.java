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
package ch.digitalfondue.stampo.processor;

import static java.util.Optional.empty;
import static java.util.Optional.of;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Optional;

import ch.digitalfondue.stampo.StampoGlobalConfiguration;
import ch.digitalfondue.stampo.resource.FileMetadata;
import ch.digitalfondue.stampo.resource.FileResource;
import ch.digitalfondue.stampo.resource.Resource;
import ch.digitalfondue.stampo.resource.StructuredFileExtension;

public class FileResourcePlaceHolder implements FileResource {

  private final StampoGlobalConfiguration conf;
  private final Path path;

  public FileResourcePlaceHolder(Path path, StampoGlobalConfiguration conf) {
    this.path = path;
    this.conf = conf;
  }

  @Override
  public Resource getParent() {
    return null;
  }

  @Override
  public Path getPath() {
    return path;
  }

  @Override
  public StampoGlobalConfiguration getConfiguration() {
    return conf;
  }

  @Override
  public FileMetadata getMetadata() {
    return new FileMetadata(Collections.emptyMap());
  }

  @Override
  public Optional<String> getContent() {
    return of("");
  }

  @Override
  public StructuredFileExtension getStructuredFileExtension() {
    return new StructuredFileExtension(Collections.emptyList(), empty(), of("html"),
        Collections.emptySet(), Collections.emptyList());
  }
}