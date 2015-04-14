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

import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.newBufferedWriter;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import ch.digitalfondue.stampo.ProcessedInputHandler;
import ch.digitalfondue.stampo.StampoGlobalConfiguration;
import ch.digitalfondue.stampo.exception.ConfigurationException;
import ch.digitalfondue.stampo.resource.Directory;
import ch.digitalfondue.stampo.resource.DirectoryPaginationConfiguration;
import ch.digitalfondue.stampo.resource.FileMetadata;
import ch.digitalfondue.stampo.resource.FileResource;
import ch.digitalfondue.stampo.resource.TaxonomyPaginationConfiguration;

//TODO: cleanup and break up the class
public class ResourceProcessor {
  
  private static final String PAGE_DIRECTORY_NAME = "page";

  private final LayoutProcessor layoutProcessor;
  private final FileResourceProcessor fileResourceProcessor;
  private final Directory root;
  private final StampoGlobalConfiguration configuration;
  private final Path outputDir;

  public ResourceProcessor(Path outputDir, Directory root, StampoGlobalConfiguration configuration) {

    this.root = root;
    this.configuration = configuration;
    this.outputDir = outputDir;

    this.fileResourceProcessor = new FileResourceProcessor(configuration, outputDir, root);
    this.layoutProcessor = new LayoutProcessor(configuration, root, fileResourceProcessor);
  }
  
  private static class PathAndModelSupplier {
    
    final Path outputPath;
    final Supplier<Map<String, Object>> modelSupplier;
    
    PathAndModelSupplier(Path outputPath, Supplier<Map<String, Object>> model) {
      this.outputPath = outputPath;
      this.modelSupplier = model;
    }
  }
  
  public static class PageContent {
    private final FileResource resource;
    private final String renderedResource;
    private final String relativeUrlToContent;
    
    public PageContent(FileResource resource, String renderedResource, String relativeUrlToContent) {
      this.resource = resource;
      this.renderedResource = renderedResource;
      this.relativeUrlToContent = relativeUrlToContent;
    }

    public FileResource getResource() {
      return resource;
    }

    public String getRenderedResource() {
      return renderedResource;
    }

    public String getRelativeUrlToContent() {
      return relativeUrlToContent;
    }

  }
  
  public static class Page {
    
    private final long currentPage;
    private final long pageSize;
    private final long pageCount;
    private final long totalItemCount;
    private final BiFunction<Long, Long, String> pageNameGenerator;
    private final List<PageContent> pageContent;
    
    public Page(long currentPage, long pageSize, long pageCount, long totalItemCount, BiFunction<Long, Long, String> pageNameGenerator, List<PageContent> pageContent) {
      this.currentPage = currentPage;
      this.pageSize = pageSize;
      this.pageCount = pageCount;
      this.totalItemCount = totalItemCount;
      this.pageNameGenerator = pageNameGenerator;
      this.pageContent = pageContent;
    }
    
    public long getCurrentPage() {
      return currentPage;
    }
    
    public long getPageSize() {
      return pageSize;
    }
    
    public long getPageCount() {
      return pageCount;
    }
    
    public long getTotalItemCount() {
      return totalItemCount;
    }
    
    public boolean isFirstPage() {
      return currentPage == 1;
    }
    
    public boolean isLastPage() {
      return currentPage == pageCount;
    }
    
    public String getPreviousPageRelativeLink() {
      return pageNameGenerator.apply(currentPage, currentPage - 1);
    }
    
    public String getNextPageRelativeLink() {
      return pageNameGenerator.apply(currentPage, currentPage + 1);
    }

    public List<PageContent> getPageContent() {
      return pageContent;
    }
  }


  public void process(FileResource resource, Locale locale, ProcessedInputHandler outputHandler) {

    FileMetadata metadata = resource.getMetadata();
    
    Locale finalLocale = metadata.getOverrideLocale().orElse(locale);
    
    Optional<DirectoryPaginationConfiguration> dirPagination = metadata.getDirectoryPaginationConfiguration();
    Optional<TaxonomyPaginationConfiguration> taxonomyPagination = metadata.getTaxonomyPaginationConfiguration();
    
    if(dirPagination.isPresent() && taxonomyPagination.isPresent()) {
      throw new ConfigurationException(resource.getPath(), "cannot have both "
          + FileMetadata.METADATA_PAGINATE_OVER_DIRECTORY + " and "
          + FileMetadata.METADATA_PAGINATE_OVER_TAXONOMY + " attribute");
    }
    List<PathAndModelSupplier> outputPaths;
    
    
    Path defaultOutputPath = extractOutputPath(resource);
    
    //TODO: this pagination strategy could be moved inside the FileResource (?)
    if (dirPagination.isPresent()) {
      outputPaths = handleDirPagination(resource, locale, dirPagination.get(), defaultOutputPath);
    } else if (taxonomyPagination.isPresent()) {
      throw new UnsupportedOperationException("pagination over taxonomy not supported at the moment");
    } else {
      outputPaths = Collections.singletonList(new PathAndModelSupplier(defaultOutputPath, Collections::emptyMap));
    }
    //
    outputPaths.forEach(outputPathAndModel -> processToPath(resource, outputHandler, finalLocale, outputPathAndModel.outputPath, outputPathAndModel.modelSupplier));
    
  }


  private Path extractOutputPath(FileResource resource) {
    FileMetadata metadata = resource.getMetadata();
    Path defaultOutputPath = metadata.getOverrideOutputToPath().map(outputDir::resolve)
        .orElse(fileResourceProcessor.normalizeOutputPath(resource));
    return defaultOutputPath;
  }
  

  private List<PathAndModelSupplier> handleDirPagination(FileResource resource, Locale locale,
      DirectoryPaginationConfiguration directoryPaginationConfiguration, Path defaultOutputPath) {
    
    List<PathAndModelSupplier> outpuPaths = new ArrayList<PathAndModelSupplier>();
    
    
    Path basePageDir = defaultOutputPath.getParent().resolve(PAGE_DIRECTORY_NAME);
    
    Path targetDirPath = configuration.getBaseDirectory().resolve(leftTrimSlash(directoryPaginationConfiguration.getBaseDirectory()));
    
    checkTargetDirectory(resource, directoryPaginationConfiguration, targetDirPath);

    
    //TODO: apply filter
    
    if (targetDirPath.startsWith(configuration.getContentDir())) {
      root.getDirectory(configuration.getContentDir().relativize(targetDirPath)).ifPresent((dir) -> {
        List<FileResource> files = dir.getFiles().values().stream()
            .filter(f-> {
              FileMetadata m = f.getMetadata();
              //filter out the files with pagination
              return !m.getTaxonomyPaginationConfiguration().isPresent() && !m.getDirectoryPaginationConfiguration().isPresent();
            }).collect(Collectors.toList()); 
        
        long count = files.size();
        long pageSize = directoryPaginationConfiguration.getPageSize();
        
        //we already have the "index page"
        long additionalPages = Math.max((count / pageSize + (count % pageSize > 0 ? 1 : 0)) - 1, 0);
        
        Supplier<Map<String, Object>> indexPageModelSupplier = buildModelForPage(1, pageSize, additionalPages, files, defaultOutputPath, defaultOutputPath, resource, locale);
        outpuPaths.add(new PathAndModelSupplier(defaultOutputPath, indexPageModelSupplier));//fixme model
        
        for(int i = 0; i < additionalPages; i++) {
          Path pageOutputPath = basePageDir.resolve(pageName(i + 2, resource));
          Supplier<Map<String, Object>> pageModelSupplier = buildModelForPage(i + 2, pageSize, additionalPages, files, defaultOutputPath, pageOutputPath, resource, locale);
          outpuPaths.add(new PathAndModelSupplier(pageOutputPath, pageModelSupplier));
        }
      });
    } else {
      //static / content / other
      //FIXME
      throw new UnsupportedOperationException("pagination over 'not content' dir not supported at the moment");
    }
    
    
    return outpuPaths;
  }
  
  private Supplier<Map<String, Object>> buildModelForPage(long page, long pageSize,
      long additionalPages, List<FileResource> files, Path defaultOutputPath, Path pagePath,
      FileResource fileResource, Locale locale) {
    return () -> {
      Map<String, Object> model = new HashMap<>();
      List<PageContent> pageContent = files.stream().skip((page - 1) * pageSize).limit(pageSize).map(f -> toPageContent(f, locale, pagePath)).collect(Collectors.toList());
      BiFunction<Long, Long, String> paginationFunction = urlPaginationGenerator(defaultOutputPath, fileResource);
      model.put("pagination", new Page(page, pageSize, additionalPages + 1, files.size(), paginationFunction, pageContent));
      return model;
    };
  }
  
  private PageContent toPageContent(FileResource fileResource, Locale locale, Path pagePath) {
    
    Map<String, Object> model =
        ModelPreparer.prepare(root, configuration, locale, fileResource, Collections.emptyMap());

    FileResourceProcessorOutput processed =
        fileResourceProcessor.applyProcessors(fileResource, locale, model);
    
    Path outputPath = extractOutputPath(fileResource);
    
    //
    if("index.html".equals(outputPath.getFileName().toString())) {
      outputPath = outputPath.getParent();
    }
    
    if("index.html".equals(pagePath.getFileName().toString())) {
      pagePath = pagePath.getParent();
    }
    //
    
    return new PageContent(fileResource, processed.getContent(), pagePath.relativize(outputPath).toString());
  }
  
  private BiFunction<Long, Long, String> urlPaginationGenerator(Path defaultOutputPath,
      FileResource fileResource) {
    return (currentPage, nextPageToLink) -> {
      // handle override + ugly url
      String url;
      if (currentPage == 1) {
        url = PAGE_DIRECTORY_NAME + "/" + pageName(nextPageToLink, fileResource);
      } else if (currentPage == 2 && nextPageToLink == 1) {
        url = (useUglyUrl(fileResource) ? "" : "../") + "../" + (defaultOutputPath.getFileName());
      } else {
        url = (useUglyUrl(fileResource) ? "" : "../") + pageName(nextPageToLink, fileResource);
      }
      return removeIndexHtml(url);
    };
  }
  
  private static String removeIndexHtml(String s) {
    return s.replaceFirst("/index\\.html$", "/");
  }
  
  private boolean useUglyUrl(FileResource fileResource) {
    return fileResource.getMetadata().getOverrideUseUglyUrl().orElse(configuration.useUglyUrl());
  }
  
  private String pageName(long pageNumber, FileResource fileResource) {
    if (useUglyUrl(fileResource)) {
      return pageNumber + ".html";
    } else {
      return pageNumber + "/index.html";
    }
  }


  private void checkTargetDirectory(FileResource resource,
      DirectoryPaginationConfiguration directoryPaginationConfiguration, Path targetDir) {
    if (!targetDir.startsWith(configuration.getBaseDirectory())) {
      throw new ConfigurationException(resource.getPath(),
          "selected directory for pagination must be inside target dir");
    }

    if (!Files.exists(targetDir)) {
      throw new ConfigurationException(resource.getPath(), "target directory "
          + directoryPaginationConfiguration.getBaseDirectory() + " must exists");
    }
  }
  
  private static String leftTrimSlash(String s) {
    return s.startsWith("/") ? s.substring(1) : s;  
  }


  private void processToPath(FileResource resource, ProcessedInputHandler outputHandler,
      Locale finalLocale, Path outputPath, Supplier<Map<String,Object>> additionalData) {
    
    
    if (!outputPath.startsWith(outputDir)) {
      throw new IllegalStateException("output path " + outputPath
          + " must be a child of outputDir: " + outputDir
          + " (override-output-to-path must be a relative path: it must not begin with \"/\")");
    }

    try {
      // ensure presence of base directory
      createDirectories(outputPath.getParent());
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
    
    Map<String, Object> model = ModelPreparer.prepare(root, configuration, finalLocale, resource, additionalData.get());

    FileResourceProcessorOutput processed =
        fileResourceProcessor.applyProcessors(resource, finalLocale, model);
    
    Map<String, Object> layoutModel = new HashMap<String, Object>(model);
    
    layoutModel.put("content", processed.getContent());

    LayoutProcessorOutput processedLayout =
        layoutProcessor.applyLayout(resource, finalLocale, layoutModel);

    try (Writer writer =
        newBufferedWriter(outputPath, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW,
            StandardOpenOption.WRITE)) {
      writer.write(outputHandler.apply(processed, processedLayout));
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
