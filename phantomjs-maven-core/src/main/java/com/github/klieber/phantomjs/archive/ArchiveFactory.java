/*-
 * #%L
 * PhantomJS Maven Core
 * %%
 * Copyright (C) 2013 - 2017 Kyle Lieber
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */
package com.github.klieber.phantomjs.archive;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.klieber.phantomjs.archive.mapping.ArchiveMapping;
import com.github.klieber.phantomjs.archive.mapping.ArchiveMappings;
import com.github.klieber.phantomjs.resolve.PhantomJsResolverOptions;
import com.github.klieber.phantomjs.sys.os.OperatingSystem;
import com.github.klieber.phantomjs.sys.os.OperatingSystemFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.net.URL;

@Named
public class ArchiveFactory {

  private static final Logger LOGGER = LoggerFactory.getLogger(ArchiveFactory.class);

  private final OperatingSystemFactory operatingSystemFactory;

  @Inject
  public ArchiveFactory(OperatingSystemFactory operatingSystemFactory) {
    this.operatingSystemFactory = operatingSystemFactory;
  }

  public Archive create(PhantomJsResolverOptions options) {
    return create(options.getVersion(), options.getBaseUrl());
  }

  public Archive create(String version, String baseUrl) {
    Archive archive = create(version);

    if (baseUrl != null) {
      archive = new CustomBaseUrlArchive(archive, baseUrl);
    }

    return archive;
  }

  public Archive create(String version) {

    OperatingSystem operatingSystem = this.operatingSystemFactory.create();

    Archive archive = null;

    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    URL resource = ArchiveFactory.class.getResource("/archive-mapping.yaml");
    try {
      ArchiveMappings archiveMappings = mapper.readValue(resource, ArchiveMappings.class);
      for (ArchiveMapping archiveMapping : archiveMappings.getMappings()) {
        if (archiveMapping.getSpec().matches(version, operatingSystem)) {
          archive = new TemplatedArchive(archiveMapping.getFormat(), version);
          break;
        }
      }
    } catch (IOException e) {
      LOGGER.error("Unable to read archive-mapping.yaml", e);
    }
    if (archive == null) {
      throw new UnsupportedPlatformException(operatingSystem);
    }
    return archive;
  }
}
