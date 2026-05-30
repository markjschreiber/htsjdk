# HTSJDK 4.0 Migration Guide: File to Path

## Overview

HTSJDK 4.0.0 completes the migration from `java.io.File` to `java.nio.file.Path` across the entire
public API. All `File`-based methods, constructors, and parameters have been removed from the main
source tree. This is a breaking change that requires downstream projects to update their code.

**Why?** The `java.nio.file.Path` API supports pluggable filesystem providers via Java's NIO SPI
mechanism. This means HTSJDK can now transparently work with:

- Local filesystems (default)
- In-memory filesystems (e.g., Google jimfs for testing)
- Cloud storage (e.g., Amazon S3, Google Cloud Storage, Azure Blob via NIO providers)
- Distributed filesystems (e.g., HDFS via NIO provider)

## Removed File-Based Methods by Class

### `htsjdk.samtools.util.IOUtil`

| Removed Method | Replacement |
|---|---|
| `IOUtil.assertFileIsReadable(File)` | `IOUtil.assertFileIsReadable(Path)` |
| `IOUtil.assertFileIsWritable(File)` | `IOUtil.assertFileIsWritable(Path)` |
| `IOUtil.assertFilesAreReadable(List<File>)` | `IOUtil.assertPathsAreReadable(List<Path>)` |
| `IOUtil.assertFilesAreWritable(List<File>)` | `IOUtil.assertPathsAreWritable(List<Path>)` |
| `IOUtil.assertDirectoryIsReadable(File)` | `IOUtil.assertDirectoryIsReadable(Path)` |
| `IOUtil.assertDirectoryIsWritable(File)` | `IOUtil.assertDirectoryIsWritable(Path)` |
| `IOUtil.openFileForReading(File)` | `IOUtil.openFileForReading(Path)` |
| `IOUtil.openFileForWriting(File)` | `IOUtil.openFileForWriting(Path)` |
| `IOUtil.openFileForWriting(File, boolean)` | `IOUtil.openFileForWriting(Path, OpenOption...)` |
| `IOUtil.openFileForBufferedReading(File)` | `IOUtil.openFileForBufferedReading(Path)` |
| `IOUtil.openFileForBufferedWriting(File)` | `IOUtil.openFileForBufferedWriting(Path)` |
| `IOUtil.openGzipFileForReading(File)` | `IOUtil.openGzipFileForReading(Path)` |
| `IOUtil.openGzipFileForWriting(File)` | `IOUtil.openGzipFileForWriting(Path)` |
| `IOUtil.deleteFiles(File...)` | `IOUtil.deletePaths(Path...)` |
| `IOUtil.getDefaultTmpDir()` returning `File` | `IOUtil.getDefaultTmpDirPath()` returning `Path` |
| `IOUtil.fileSizeViaChannel(File)` | `IOUtil.fileSizeViaChannel(Path)` |

### `htsjdk.samtools.SamReaderFactory`

| Removed Method | Replacement |
|---|---|
| `open(File)` | `open(Path)` or `open(URI)` |
| `open(File, SamInputResource)` | `open(Path)` with `SamInputResource` |
| `getFileHeader(File)` | `getFileHeader(Path)` |

### `htsjdk.samtools.SAMFileWriterFactory`

| Removed Method | Replacement |
|---|---|
| `makeBAMWriter(SAMFileHeader, boolean, File)` | `makeBAMWriter(SAMFileHeader, boolean, Path)` |
| `makeBAMWriter(SAMFileHeader, boolean, File, int)` | `makeBAMWriter(SAMFileHeader, boolean, Path, int)` |
| `makeSAMWriter(SAMFileHeader, boolean, File)` | `makeSAMWriter(SAMFileHeader, boolean, Path)` |
| `makeSAMOrBAMWriter(SAMFileHeader, boolean, File)` | `makeSAMOrBAMWriter(SAMFileHeader, boolean, Path)` |
| `makeCRAMWriter(SAMFileHeader, File, File)` | `makeCRAMWriter(SAMFileHeader, Path, Path)` |
| `setTempDirectory(File)` | `setTempDirectory(Path)` |

### `htsjdk.samtools.reference.ReferenceSequenceFileFactory`

| Removed Method | Replacement |
|---|---|
| `getReferenceSequenceFile(File)` | `getReferenceSequenceFile(Path)` or `getReferenceSequenceFile(URI)` |
| `getReferenceSequenceFile(File, boolean)` | `getReferenceSequenceFile(Path, boolean)` |
| `getReferenceSequenceFile(File, boolean, boolean)` | `getReferenceSequenceFile(Path, boolean, boolean)` |

### `htsjdk.samtools.Defaults`

| Removed Field | Replacement |
|---|---|
| `REFERENCE_FASTA` (type `File`) | `REFERENCE_FASTA` (type `Path`) |

### `htsjdk.samtools.BAMIndexer`

| Removed Method | Replacement |
|---|---|
| `BAMIndexer(File, SAMFileHeader)` | `BAMIndexer(Path, SAMFileHeader)` |
| `createIndex(SamReader, File)` | `createIndex(SamReader, Path)` |

### `htsjdk.samtools.SamFiles`

| Removed Method | Replacement |
|---|---|
| `findIndex(File)` | `findIndex(Path)` |

### `htsjdk.variant.vcf.VCFFileReader`

| Removed Method | Replacement |
|---|---|
| `VCFFileReader(File)` | `VCFFileReader(Path)` |
| `VCFFileReader(File, boolean)` | `VCFFileReader(Path, boolean)` |
| `VCFFileReader(File, File, boolean)` | `VCFFileReader(Path, Path, boolean)` |

### `htsjdk.tribble.AbstractFeatureReader`

| Removed Method | Replacement |
|---|---|
| `getFeatureReader(File, FeatureCodec, boolean)` | `getFeatureReader(Path, FeatureCodec, boolean)` |


## Before/After Code Examples

### Opening a BAM File

**Before (3.x):**
```java
import java.io.File;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;

File bamFile = new File("/path/to/input.bam");
SamReader reader = SamReaderFactory.makeDefault().open(bamFile);
```

**After (4.0):**
```java
import java.nio.file.Path;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;

Path bamPath = Path.of("/path/to/input.bam");
SamReader reader = SamReaderFactory.makeDefault().open(bamPath);
```

### Creating a BAM Writer

**Before (3.x):**
```java
import java.io.File;
import htsjdk.samtools.SAMFileWriterFactory;
import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMFileHeader;

File outputFile = new File("/path/to/output.bam");
SAMFileWriter writer = new SAMFileWriterFactory()
    .makeBAMWriter(header, true, outputFile);
```

**After (4.0):**
```java
import java.nio.file.Path;
import htsjdk.samtools.SAMFileWriterFactory;
import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMFileHeader;

Path outputPath = Path.of("/path/to/output.bam");
SAMFileWriter writer = new SAMFileWriterFactory()
    .makeBAMWriter(header, true, outputPath);
```

### Working with Indexes

**Before (3.x):**
```java
import java.io.File;
import htsjdk.samtools.SamFiles;
import htsjdk.samtools.BAMIndexer;

File bamFile = new File("input.bam");
File indexFile = SamFiles.findIndex(bamFile);

// Creating an index
BAMIndexer.createIndex(reader, new File("output.bai"));
```

**After (4.0):**
```java
import java.nio.file.Path;
import htsjdk.samtools.SamFiles;
import htsjdk.samtools.BAMIndexer;

Path bamPath = Path.of("input.bam");
Path indexPath = SamFiles.findIndex(bamPath);

// Creating an index
BAMIndexer.createIndex(reader, Path.of("output.bai"));
```

### Using URIs

**After (4.0) — new capability:**
```java
import java.net.URI;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;

// Open a BAM file from a URI (requires appropriate NIO provider)
URI s3Uri = URI.create("s3://my-bucket/samples/sample1.bam");
SamReader reader = SamReaderFactory.makeDefault().open(s3Uri);

// Open a reference from a URI
URI refUri = URI.create("s3://my-bucket/references/hg38.fasta");
ReferenceSequenceFile ref = ReferenceSequenceFileFactory.getReferenceSequenceFile(refUri);
```

### Using Custom Filesystems (jimfs)

**After (4.0) — in-memory testing:**
```java
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;

// Create an in-memory filesystem for testing
try (FileSystem fs = Jimfs.newFileSystem(Configuration.unix())) {
    Path bamPath = fs.getPath("/test/input.bam");
    Files.copy(realBamStream, bamPath);

    SamReader reader = SamReaderFactory.makeDefault().open(bamPath);
    // ... test logic ...
}
```

### Reading a VCF File

**Before (3.x):**
```java
import java.io.File;
import htsjdk.variant.vcf.VCFFileReader;

VCFFileReader reader = new VCFFileReader(new File("variants.vcf.gz"), true);
```

**After (4.0):**
```java
import java.nio.file.Path;
import htsjdk.variant.vcf.VCFFileReader;

VCFFileReader reader = new VCFFileReader(Path.of("variants.vcf.gz"), true);
```

## Common Migration Patterns

Use these search-and-replace patterns to update your code:

| Old Pattern | New Pattern |
|---|---|
| `new File("path")` | `Path.of("path")` |
| `new File(parent, child)` | `parent.resolve(child)` |
| `file.exists()` | `Files.exists(path)` |
| `file.isFile()` | `Files.isRegularFile(path)` |
| `file.isDirectory()` | `Files.isDirectory(path)` |
| `file.length()` | `Files.size(path)` |
| `file.delete()` | `Files.delete(path)` or `Files.deleteIfExists(path)` |
| `file.getName()` | `path.getFileName().toString()` |
| `file.getParent()` | `path.getParent().toString()` |
| `file.getAbsolutePath()` | `path.toAbsolutePath().toString()` |
| `file.toPath()` | *(already a Path — no change needed)* |
| `path.toFile()` | *(remove — not needed with Path API)* |
| `file.mkdirs()` | `Files.createDirectories(path)` |
| `file.createNewFile()` | `Files.createFile(path)` |
| `file.canRead()` | `Files.isReadable(path)` |
| `file.canWrite()` | `Files.isWritable(path)` |
| `file.listFiles()` | `Files.list(path)` (returns `Stream<Path>`) |
| `new FileInputStream(file)` | `Files.newInputStream(path)` |
| `new FileOutputStream(file)` | `Files.newOutputStream(path)` |

### Import Changes

Replace:
```java
import java.io.File;
```

With:
```java
import java.nio.file.Path;
import java.nio.file.Files;
```

## NIO Filesystem Provider (SPI) Support

HTSJDK 4.0 fully supports Java's NIO Service Provider Interface for custom filesystems.
To use a custom filesystem:

1. Add the NIO provider dependency to your project (e.g., `google-cloud-nio` for GCS,
   `nio-s3` for S3, `hadoop-nio` for HDFS).
2. The provider registers itself via `META-INF/services/java.nio.file.spi.FileSystemProvider`.
3. Use URIs or Paths from the custom filesystem directly with HTSJDK APIs.

No HTSJDK-specific configuration is required — the standard Java SPI mechanism handles discovery.

## Troubleshooting

### `FileSystemNotFoundException`

If you get this exception when using a URI, it means no NIO provider is registered for that
URI scheme. Ensure the appropriate provider JAR is on your classpath.

### `UnsupportedOperationException` from `Path.toFile()`

Some code may call `toFile()` on a Path from a non-default filesystem. This is not supported
by custom filesystem providers. Remove any `toFile()` calls and use `Path`/`Files` methods
directly.

### Gradle/Maven Dependency

```groovy
// Gradle
implementation 'com.github.samtools:htsjdk:4.0.0'
```

```xml
<!-- Maven -->
<dependency>
    <groupId>com.github.samtools</groupId>
    <artifactId>htsjdk</artifactId>
    <version>4.0.0</version>
</dependency>
```
