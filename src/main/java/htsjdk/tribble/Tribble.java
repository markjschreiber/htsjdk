/*
 * The MIT License
 *
 * Copyright (c) 2013 The Broad Institute
 *
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
 */
package htsjdk.tribble;

import htsjdk.samtools.util.FileExtensions;
import htsjdk.tribble.util.ParsingUtils;

import java.nio.file.Path;

/**
 * Common Tribble-wide constants and static functions for working with genomic feature files.
 * 
 * <p>This class provides utilities for constructing index file paths for Tribble and Tabix indexes.
 * All methods use {@link Path} for filesystem operations, enabling compatibility with Java NIO 
 * Service Provider Interface (SPI) and custom filesystems.
 */
public class Tribble {
    private Tribble() { } // can't be instantiated

    /**
     * @deprecated since June 2019 Use {@link FileExtensions#TRIBBLE_INDEX} instead.
     */
    @Deprecated
    public final static String STANDARD_INDEX_EXTENSION = FileExtensions.TRIBBLE_INDEX;

    /**
     * Return the name of the index file for the provided {@code filename}.
     * Does not actually create an index.
     * 
     * @param filename  name of the file
     * @return non-null String representing the index filename
     */
    public static String indexFile(final String filename) {
        return indexFile(filename, FileExtensions.TRIBBLE_INDEX);
    }

    /**
     * Return the Path of the index file for the provided {@code path}.
     * Does not actually create an index.
     * 
     * <p>The index path is constructed by appending the Tribble index extension 
     * ({@link FileExtensions#TRIBBLE_INDEX}) to the absolute path.
     * 
     * @param path the path to the data file
     * @return a non-null Path representing the index file location
     */
    public static Path indexPath(final Path path) {
        return path.getFileSystem().getPath(indexFile(path.toAbsolutePath().toString()));
    }

    /**
     * Return the name of the tabix index file for the provided {@code filename}.
     * Does not actually create an index.
     * 
     * @param filename  name of the file
     * @return non-null String representing the tabix index filename
     */
    public static String tabixIndexFile(final String filename) {
        return indexFile(filename, FileExtensions.TABIX_INDEX);
    }

    /**
     * Return the Path of the tabix index file for the provided {@code path}.
     * Does not actually create an index.
     * 
     * <p>The tabix index path is constructed by appending the tabix index extension 
     * ({@link FileExtensions#TABIX_INDEX}) to the absolute path.
     * 
     * @param path the path to the data file
     * @return a non-null Path representing the tabix index file location
     */
    public static Path tabixIndexPath(final Path path) {
        return path.getFileSystem().getPath(tabixIndexFile(path.toAbsolutePath().toString()));
    }

    /**
     * Return the name of the index file for the provided {@code filename} and {@code extension}.
     * Does not actually create an index.
     * 
     * @param filename  name of the file
     * @param extension the extension to use for the index
     * @return non-null String representing the index filename
     */
    private static String indexFile(final String filename, final String extension) {
        return ParsingUtils.appendToPath(filename, extension);
    }
}
