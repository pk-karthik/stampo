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

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import ch.digitalfondue.stampo.resource.FileResource;

public class DefaultDirective implements Directive {

  @Override
  public List<PathAndModelSupplier> generateOutputPaths(FileResource resource, Locale locale,
      Path defaultOutputPath) {
    return Collections.singletonList(new PathAndModelSupplier(defaultOutputPath,
        Collections::emptyMap));
  }

  @Override
  public String name() {
    return "default";
  }

}
