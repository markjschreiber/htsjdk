# File to Path Migration Example

This document shows concrete examples of how to migrate from `java.io.File` to `java.nio.file.Path` with URI support.

## Example 1: SamReaderFactory Migration

### Before (Current Code)

```java
package htsjdk.samtools;

import java.io.File;
import java.nio.file.Path;

public abstract class SamReaderFactory {
    
    // File-based method (to be removed)
    abstract public SamReader open(final File file);
    
    // Path-based method (already exists)
    public SamReader open(final Path path) {
        return open(path, null, null);
    }
    
    // Reference sequence with File
    abstract public SamReaderFactory referenceSequence(File referenceSequence);
    
    // Reference sequence with Path (already exists)
    abstract public SamReaderFactory referenceSequence(Path referenceSequence);
    
    // Header utility with File
    abstract public SAMFileHeader getFileHeader(File samFile);
    
    // Header utility with Path (already exists)
    abstract public SAMFileHeader getFileHeader(Path samFile);
}
```

### After (Migrated Code)

```java
package htsjdk.samtools;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.channels.SeekableByteChannel;
import java.util.function.Function;

public abstract class SamReaderFactory {
    
    // REMOVED: abstract public SamReader open(final File file);
    
    /**
     * Open the specified path (without using any wrappers).
     *
     * @param path the SAM or BAM file to open.
     * @return a SamReader for the specified path
     */
    public SamReader open(final Path path) {
        return open(path, null, null);
    }
    
    /**
     * Open a SAM/BAM/CRAM file from a URI.
     * Supports file://, http://, https://, ftp://, and custom NIO filesystem URIs.
     *
     * @param uri the URI of the SAM or BAM file to open
     * @return a SamReader for the specified URI
     * @throws IOException if the file cannot be opened
     */
    public SamReader open(final URI uri) throws IOException {
        return open(Path.of(uri));
    }
    
    /**
     * Open a SAM/BAM/CRAM file from a string path.
     * For URIs, use {@link #open(URI)} instead.
     *
     * @param pathString the path string
     * @return a SamReader for the specified path
     */
    public SamReader open(final String pathString) {
        return open(Path.of(pathString));
    }
    
    /**
     * Open the specified path, using the specified wrappers for prefetching/caching.
     *
     * @param path the SAM or BAM file to open
     * @param dataWrapper the wrapper for the data (or null for none)
     * @param indexWrapper the wrapper for the index (or null for none)
     * @return a SamReader for the specified path
     */
    public SamReader open(final Path path,
            Function<SeekableByteChannel, SeekableByteChannel> dataWrapper,
            Function<SeekableByteChannel, SeekableByteChannel> indexWrapper) {
        final SamInputResource r = dataWrapper == null
                ? SamInputResource.of(path)
                : SamInputResource.of(path, dataWrapper);
        final Path indexMaybe = SamFiles.findIndex(path);
        if (indexMaybe != null) r.index(indexMaybe, indexWrapper);
        return open(r);
    }
    
    abstract public SamReader open(final SamInputResource resource);
    
    // REMOVED: abstract public SamReaderFactory referenceSequence(File referenceSequence);
    
    /**
     * Sets the reference sequence file.
     *
     * @param referenceSequence path to the reference sequence
     * @return this factory
     */
    abstract public SamReaderFactory referenceSequence(Path referenceSequence);
    
    /**
     * Sets the reference sequence from a URI.
     *
     * @param referenceUri URI of the reference sequence
     * @return this factory
     * @throws IOException if the URI cannot be resolved
     */
    public SamReaderFactory referenceSequence(URI referenceUri) throws IOException {
        return referenceSequence(Path.of(referenceUri));
    }
    
    // REMOVED: abstract public SAMFileHeader getFileHeader(File samFile);
    
    /**
     * Utility method to open the file, get the header, and close the file.
     *
     * @param samPath path to the SAM/BAM/CRAM file
     * @return the file header
     */
    abstract public SAMFileHeader getFileHeader(Path samPath);
    
    /**
     * Utility method to open the file from URI, get the header, and close the file.
     *
     * @param samUri URI of the SAM/BAM/CRAM file
     * @return the file header
     * @throws IOException if the file cannot be opened
     */
    public SAMFileHeader getFileHeader(URI samUri) throws IOException {
        return getFileHeader(Path.of(samUri));
    }
}
```

### Migration Notes

1. **Removed Methods**: All `File`-based abstract methods removed
2. **Added URI Support**: New `open(URI)` and convenience methods
3. **String Convenience**: Added `open(String)` for simple paths
4. **Consistent API**: All methods now use Path as the primary type

## Example 2: Index File Naming (Tribble)

### Before

```java
package htsjdk.tribble;

import java.io.File;

public class Tribble {
    
    public static File indexFile(final File file) {
        return indexFile(file.getAbsoluteFile(), FileExtensions.TRIBBLE_INDEX);
    }
    
    public static File tabixIndexFile(final File file) {
        return indexFile(file.getAbsoluteFile(), FileExtensions.TABIX_INDEX);
    }
    
    private static File indexFile(final File file, final String extension) {
        return new File(file.getAbsoluteFile() + extension);
    }
}
```

### After

```java
package htsjdk.tribble;

import java.nio.file.Path;

public class Tribble {
    
    /**
     * Get the expected Tribble index path for a feature file.
     *
     * @param featurePath the feature file path
     * @return the expected index path
     */
    public static Path indexPath(final Path featurePath) {
        return indexPath(featurePath.toAbsolutePath(), FileExtensions.TRIBBLE_INDEX);
    }
    
    /**
     * Get the expected Tabix index path for a feature file.
     *
     * @param featurePath the feature file path
     * @return the expected Tabix index path
     */
    public static Path tabixIndexPath(final Path featurePath) {
        return indexPath(featurePath.toAbsolutePath(), FileExtensions.TABIX_INDEX);
    }
    
    /**
     * Construct an index path by appending an extension to a feature file path.
     *
     * @param featurePath the feature file path
     * @param extension the index extension
     * @return the index path
     */
    private static Path indexPath(final Path featurePath, final String extension) {
        return Path.of(featurePath.toString() + extension);
    }
}
```

## Example 3: Factory with File Constructor

### Before

```java
package htsjdk.samtools.reference;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class ReferenceSequenceFileFactory {
    
    public static ReferenceSequenceFile getReferenceSequenceFile(final File file) {
        return getReferenceSequenceFile(file, true);
    }
    
    public static ReferenceSequenceFile getReferenceSequenceFile(
            final File file, 
            final boolean truncateNamesAtWhitespace) {
        return getReferenceSequenceFile(file.toPath(), truncateNamesAtWhitespace);
    }
    
    public static ReferenceSequenceFile getReferenceSequenceFile(final Path path) {
        return getReferenceSequenceFile(path, true);
    }
    
    public static ReferenceSequenceFile getReferenceSequenceFile(
            final Path path,
            final boolean truncateNamesAtWhitespace) {
        // Implementation...
    }
}
```

### After

```java
package htsjdk.samtools.reference;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

public class ReferenceSequenceFileFactory {
    
    /**
     * Get a ReferenceSequenceFile for the specified path.
     *
     * @param path the reference sequence file path
     * @return a ReferenceSequenceFile instance
     * @throws IOException if the file cannot be opened
     */
    public static ReferenceSequenceFile getReferenceSequenceFile(final Path path) 
            throws IOException {
        return getReferenceSequenceFile(path, true);
    }
    
    /**
     * Get a ReferenceSequenceFile for the specified path.
     *
     * @param path the reference sequence file path
     * @param truncateNamesAtWhitespace whether to truncate sequence names at whitespace
     * @return a ReferenceSequenceFile instance
     * @throws IOException if the file cannot be opened
     */
    public static ReferenceSequenceFile getReferenceSequenceFile(
            final Path path,
            final boolean truncateNamesAtWhitespace) throws IOException {
        // Implementation using Path...
        if (!Files.exists(path)) {
            throw new IOException("Reference file not found: " + path);
        }
        // ... rest of implementation
    }
    
    /**
     * Get a ReferenceSequenceFile from a URI.
     * Supports file://, http://, https://, and custom NIO filesystem URIs.
     *
     * @param uri the reference sequence file URI
     * @return a ReferenceSequenceFile instance
     * @throws IOException if the file cannot be opened
     */
    public static ReferenceSequenceFile getReferenceSequenceFile(final URI uri) 
            throws IOException {
        return getReferenceSequenceFile(Path.of(uri), true);
    }
    
    /**
     * Get a ReferenceSequenceFile from a URI.
     *
     * @param uri the reference sequence file URI
     * @param truncateNamesAtWhitespace whether to truncate sequence names at whitespace
     * @return a ReferenceSequenceFile instance
     * @throws IOException if the file cannot be opened
     */
    public static ReferenceSequenceFile getReferenceSequenceFile(
            final URI uri,
            final boolean truncateNamesAtWhitespace) throws IOException {
        return getReferenceSequenceFile(Path.of(uri), truncateNamesAtWhitespace);
    }
}
```

## Example 4: Test Migration

### Before

```java
import java.io.File;

public class SAMFileReaderTest {
    
    @Test
    public void testReadBamFile() throws IOException {
        final File bamFile = new File("src/test/resources/htsjdk/samtools/example.bam");
        final SamReader reader = SamReaderFactory.makeDefault().open(bamFile);
        // ... test code
    }
}
```

### After

```java
import java.net.URI;
import java.nio.file.Path;

public class SAMFileReaderTest {
    
    @Test
    public void testReadBamFile() throws IOException {
        // Option 1: Using Path directly
        final Path bamPath = Path.of("src/test/resources/htsjdk/samtools/example.bam");
        final SamReader reader = SamReaderFactory.makeDefault().open(bamPath);
        
        // Option 2: Using URI (preferred for test resources)
        final URI bamUri = getClass().getResource("/htsjdk/samtools/example.bam").toURI();
        final SamReader reader2 = SamReaderFactory.makeDefault().open(bamUri);
        
        // Option 3: Using Path.of(URI) for custom filesystems
        final Path bamPath2 = Path.of(bamUri);
        final SamReader reader3 = SamReaderFactory.makeDefault().open(bamPath2);
        
        // ... test code
    }
    
    @Test
    public void testWithCustomFileSystem() throws IOException {
        // Test with jimfs (in-memory filesystem)
        try (FileSystem fs = Jimfs.newFileSystem(Configuration.unix())) {
            final Path testFile = fs.getPath("/test.bam");
            Files.copy(Path.of("src/test/resources/htsjdk/samtools/example.bam"), testFile);
            
            // This works because we're using Path, not File!
            final SamReader reader = SamReaderFactory.makeDefault().open(testFile);
            // ... test code
        }
    }
}
```

## Common Migration Patterns

### Pattern 1: File Parameter → Path Parameter

```java
// Before
public void processFile(File inputFile) { }

// After
public void processFile(Path inputPath) { }
```

### Pattern 2: File Return → Path Return

```java
// Before
public File getOutputFile() { return outputFile; }

// After
public Path getOutputPath() { return outputPath; }
```

### Pattern 3: File.exists() → Files.exists()

```java
// Before
if (file.exists()) { }

// After
if (Files.exists(path)) { }
```

### Pattern 4: File.isDirectory() → Files.isDirectory()

```java
// Before
if (file.isDirectory()) { }

// After
if (Files.isDirectory(path)) { }
```

### Pattern 5: File.getName() → Path.getFileName()

```java
// Before
String name = file.getName();

// After
String name = path.getFileName().toString();
```

### Pattern 6: File.getAbsolutePath() → Path.toAbsolutePath()

```java
// Before
String absPath = file.getAbsolutePath();

// After
String absPath = path.toAbsolutePath().toString();
```

### Pattern 7: new File(parent, child) → parent.resolve(child)

```java
// Before
File childFile = new File(parentFile, "child.txt");

// After
Path childPath = parentPath.resolve("child.txt");
```

### Pattern 8: FileInputStream/FileOutputStream → Files.newInputStream/newOutputStream

```java
// Before
try (InputStream in = new FileInputStream(file)) { }
try (OutputStream out = new FileOutputStream(file)) { }

// After
try (InputStream in = Files.newInputStream(path)) { }
try (OutputStream out = Files.newOutputStream(path)) { }
```

## Benefits of This Migration

1. **NIO SPI Support**: Works with custom filesystems (S3, HDFS, in-memory, etc.)
2. **URI Support**: Direct support for URIs enables cloud storage and remote files
3. **Better API**: Path API is more modern and feature-rich
4. **Type Safety**: Path is more type-safe than File
5. **Performance**: NIO can be more efficient for certain operations
6. **Future-Proof**: Aligns with modern Java best practices

## Breaking Changes Summary

### Removed APIs
- All methods accepting `java.io.File` parameters
- All methods returning `java.io.File`

### Replacement APIs
- Use `java.nio.file.Path` instead
- Use `Path.of(URI)` for URI-based construction
- Use `Path.of(String)` for string paths

### Migration Path for Users

```java
// Old code (will not compile)
SamReader reader = SamReaderFactory.makeDefault().open(new File("data.bam"));

// New code - Option 1: Direct Path
SamReader reader = SamReaderFactory.makeDefault().open(Path.of("data.bam"));

// New code - Option 2: URI
SamReader reader = SamReaderFactory.makeDefault().open(URI.create("file:///path/to/data.bam"));

// New code - Option 3: Custom filesystem
Path s3Path = Path.of(URI.create("s3://bucket/data.bam"));
SamReader reader = SamReaderFactory.makeDefault().open(s3Path);
```
