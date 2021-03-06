/*******************************************************************************
 * Copyright (c) 2019, 2020 Obeo.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Obeo - initial API and implementation
 *******************************************************************************/
package org.eclipse.sirius.web.diagrams.layout;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import javax.annotation.PreDestroy;
import javax.imageio.ImageIO;

import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.bridge.BridgeContext;
import org.apache.batik.bridge.BridgeException;
import org.apache.batik.bridge.DocumentLoader;
import org.apache.batik.bridge.GVTBuilder;
import org.apache.batik.bridge.UserAgent;
import org.apache.batik.bridge.UserAgentAdapter;
import org.apache.batik.gvt.GraphicsNode;
import org.apache.batik.util.XMLResourceDescriptor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.sirius.web.diagrams.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.svg.SVGDocument;

/**
 * Service used to compute the native image size.
 *
 * @author hmarchadour
 */
@Service
public class ImageSizeService {

    private static final String SVGZ_FILE_EXTENSION = "svgz"; //$NON-NLS-1$

    private static final String SVG_FILE_EXTENSION = "svg"; //$NON-NLS-1$

    private static final long MAX_CACHE_SIZE = 1000;

    private final SAXSVGDocumentFactory factory = new SAXSVGDocumentFactory(XMLResourceDescriptor.getXMLParserClassName());

    private final UserAgent agent = new UserAgentAdapter();

    // FIXME? The loader maintains a cache which may need to be disposed on day
    private final DocumentLoader loader = new DocumentLoader(this.agent);

    private final GVTBuilder builder = new GVTBuilder();

    private final Logger logger = LoggerFactory.getLogger(ImageSizeService.class);

    private final LoadingCache<String, Optional<Size>> cache;

    public ImageSizeService() {
        var cacheLoader = new CacheLoader<String, Optional<Size>>() {
            @Override
            public Optional<Size> load(String path) throws Exception {
                return ImageSizeService.this.computeSize(path);
            }
        };

        // @formatter:off
        this.cache = CacheBuilder.newBuilder()
                .maximumSize(MAX_CACHE_SIZE)
                .recordStats()
                .build(cacheLoader);
        // @formatter:on
    }

    @PreDestroy
    public void dispose() {
        this.logger.debug(this.cache.stats().toString());
        this.cache.invalidateAll();
    }

    /**
     * Get the image size thanks to its imagePath.
     *
     * @param imagePath
     *            the image path (related to the classLoader resource path)
     * @return The native image size or or an empty optional if an issue has occurred
     */
    public Optional<Size> getSize(String imagePath) {
        Optional<Size> optionalSize = Optional.empty();

        try {
            optionalSize = this.cache.get(imagePath);
        } catch (ExecutionException exception) {
            this.logger.error(exception.getMessage(), exception);
        }

        return optionalSize;
    }

    private Optional<Size> computeSize(String imagePath) {
        Optional<Size> optionalSize = Optional.empty();

        URL url = this.getClass().getClassLoader().getResource(imagePath);
        if (url != null) {
            String extension = imagePath.substring(imagePath.lastIndexOf('.') + 1);
            if (SVG_FILE_EXTENSION.equalsIgnoreCase(extension) || SVGZ_FILE_EXTENSION.equalsIgnoreCase(extension)) {
                optionalSize = this.getSVGSize(url, extension);
            } else {
                optionalSize = this.getNonSVGSize(url);
            }
        }
        return optionalSize;
    }

    /**
     * Computes the size of the SVG image using Batik
     *
     * @param url
     *            The URL of the image
     * @param extension
     *            The extension (svg or svgz) of the image
     * @return The size of the image or an empty optional if an error has occurred
     */
    private Optional<Size> getSVGSize(URL url, String extension) {
        Optional<Size> optionalSize = Optional.empty();

        File tmpSvg = null;
        try (InputStream inJarInputStream = new BufferedInputStream(url.openStream())) {
            // We will copy the svg file to avoid jar in jar path problems.
            tmpSvg = java.io.File.createTempFile("sirius-web", extension); //$NON-NLS-1$
            try (FileOutputStream outputStream = new FileOutputStream(tmpSvg)) {
                IOUtils.copy(inJarInputStream, outputStream);
                try (InputStream onTmpInputStream = FileUtils.openInputStream(tmpSvg)) {
                    SVGDocument doc = this.factory.createSVGDocument(tmpSvg.toURI().toURL().toString(), onTmpInputStream);
                    BridgeContext context = new BridgeContext(this.agent, this.loader);
                    context.setDynamic(true);
                    GraphicsNode root = this.builder.build(context, doc);
                    /**
                     * To get bounds, we use "Geometry bounds" instead of "Primitive Bounds" or "Sensitive Bounds".
                     * Geometry bounds returns the bounds of the area (including padding).
                     *
                     * @see https://xmlgraphics.apache.org/batik/javadoc/org/apache/batik/gvt/GraphicsNode.html
                     */
                    double width = root.getGeometryBounds().getWidth();
                    double height = root.getGeometryBounds().getHeight();

                    // @formatter:off
                    optionalSize = Optional.of(Size.newSize()
                            .width(width)
                            .height(height)
                            .build());
                    // @formatter:on
                }
            }
        } catch (IOException | BridgeException exception) {
            String pattern = "An error has occured while computing the dimensions of {0}: {1}"; //$NON-NLS-1$
            this.logger.error(MessageFormat.format(pattern, url.toString(), exception.getMessage()), exception);
        } finally {
            if (tmpSvg != null && tmpSvg.exists()) {
                tmpSvg.delete();
            }
        }

        return optionalSize;
    }

    /**
     * Computes the size of the image using AWT.
     *
     * @param url
     *            The URL of the image
     * @return The size of the image or an empty optional if an error has occurred
     */
    private Optional<Size> getNonSVGSize(URL url) {
        Optional<Size> optionalSize = Optional.empty();
        try {
            BufferedImage bufferedImage = ImageIO.read(url);

            // @formatter:off
            optionalSize = Optional.of(Size.newSize()
                    .height(bufferedImage.getHeight())
                    .width(bufferedImage.getWidth())
                    .build());
            // @formatter:on
        } catch (IOException exception) {
            this.logger.error(exception.getMessage(), exception);
        }

        return optionalSize;
    }

}
