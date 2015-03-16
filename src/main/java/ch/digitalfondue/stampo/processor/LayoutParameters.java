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
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class LayoutParameters {
  public final Optional<Path> layoutTemplate;
  public final Path targetResource;
  public final Locale locale;
  public final Map<String, Object> model;

  LayoutParameters(Optional<Path> path, Path targetResource, Locale locale,
      Map<String, Object> model) {
    this.layoutTemplate = path;
    this.targetResource = targetResource;
    this.locale = locale;
    this.model = model;
  }
}
