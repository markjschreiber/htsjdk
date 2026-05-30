# Design Document: File to Path Migration

## Overview

This design document describes the migration of HTSJDK from `java.io.File` to `java.nio.file.Path` with URI constructor support. The migration enables full Java NIO Service Provider Interface (SPI) compatibility, allowing HTSJDK to work with custom filesystems such as S3, HDFS, in-memory filesystems (jimfs), and other cloud storage providers.

The migration is a breaking change suitable for HTSJDK version 4.0.0. It systematically removes all `File`-based APIs and replaces them with `Path`-based equivalents, ensuring consistent use of modern Java NIO throughout the codebase.

### Key Benefits

1. **NIO SPI Compatibility**: Works seamlessly with custom filesystem implementations
2. **URI Support**: Direct support for file://, http://, https://, and custom scheme URIs
3. **Modern API**: Path API is more feature-rich and type-safe than File
4. **Cloud-Ready**: Enables direct integration with cloud storage providers
5. **Future-Proof**: Aligns with modern Java best practices (Java 8+)

### Migration Scope

- **~50+ source files** in `src/main/java` currently import `java.io.File`
- **Hundreds of method signatures** to be updated
- **All test files** to be migrated
- **8 major package areas** affected (I/O, SAM/BAM/CRAM, VCF/BCF, FASTQ, Tribble, Reference, Utilities, Filters)

## Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     User Application                         │
└────────────────────┬────────────────────────────────────────┘
                     │ Uses Path/URI APIs
                     ▼
┌─────────────────────────────────────────────────────────────┐
│                  HTSJDK Public API Layer                     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │   Factories  │  │   Readers    │  │   Writers    │      │
│  │ (Path/URI)   │  │  (Path)      │  │  (Path)      │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└────────────────────┬────────────────────────────────────────┘
                     │ Delegates to
                     ▼
┌─────────────────────────────────────────────────────────────┐
│              Core I/O Abstraction Layer                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │   HtsPath    │  │   IOPath     │  │   IOUtil     │      │
│  │  (Path)      │  │  (Path)      │  │  (Path)      │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└────────────────────┬────────────────────────────────────────┘
                     │ Uses
                     ▼
┌─────────────────────────────────────────────────────────────┐
│              Java NIO File System API                        │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ Path.of(URI) │  │    Files     │  │  FileSystem  │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└────────────────────┬────────────────────────────────────────┘
                     │ Implemented by
                     ▼
┌─────────────────────────────────────────────────────────────┐
│           Filesystem Implementations (NIO SPI)               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ Local FS     │  │  S3 FS       │  │  HDFS        │      │
│  │ (default)    │  │  (custom)    │  │  (custom)    │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└─────────────────────────────────────────────────────────────┘
```

### Migration Strategy

The migration follows a phased approach to minimize risk and ensure incremental validation:

1. **Phase 1**: Core I/O utilities (IOUtil, HtsPath, IOPath)
2. **Phase 2**: Factory classes (SamReaderFactory, SAMFileWriterFactory, etc.)
3. **Phase 3**: Reader classes (SAM/BAM/CRAM/VCF/BCF/FASTQ readers)
4. **Phase 4**: Writer classes (SAM/BAM/CRAM/VCF/BCF/FASTQ writers)
5. **Phase 5**: Index readers (BAM, CRAM, CSI, SBI, Tribble indexes)
6. **Phase 6**: Index writers (BAMIndexer, CRAMBAIIndexer, etc.)
7. **Phase 7**: Reference sequences (FastaSequenceFile, etc.)
8. **Phase 8**: Utility classes (SamFiles, BamFileIoUtils, Tribble, Filters)
9. **Phase 9**: Test migration and NIO SPI validation

## Components and Interfaces

### Core I/O Components

#### IOUtil (htsjdk.samtools.util.IOUtil)

**Purpose**: Provides utility methods for file I/O operations.

**Key Changes**:
- Remove all File-based utility methods
- Ensure all Path-based equivalents exist
- Add URI-based Path construction helpers

**Interface**:
```java
public class IOUtil {
    // Path-based utilities
    public static Path getPath(String pathString);
    public static Path getPath(URI uri);
    public static boolean pathExists(Path path);
    public static boolean isDirectory(Path path);
    public static boolean isReadable(Path path);
    public static boolean isWritable(Path path);
    public static long sizeOf(Path path);
    public static void assertPathIsReadable(Path path);
    public static void assertPathIsWritable(Path path);
    public static void assertDirectoryIsWritable(Path directory);
    
    // NO File-based methods
}
```

#### HtsPath (htsjdk.io.HtsPath)

**Purpose**: HTSJDK-specific path abstraction with URI support.

**Key Changes**:
- Ensure exclusive use of Path internally
- Remove any File dependencies
- Support URI construction

**Interface**:
```java
public class HtsPath implements IOPath {
    public HtsPath(String pathString);
    public HtsPath(URI uri);
    public HtsPath(Path path);
    
    public Path toPath();
    public URI getURI();
    public String getURIString();
    
    // NO File-based methods or constructors
}
```

### Factory Components

#### SamReaderFactory

**Purpose**: Factory for creating SAM/BAM/CRAM readers.

**Key Changes**:
- Remove `open(File)` method
- Keep `open(Path)` method
- Add `open(URI)` convenience method
- Remove `referenceSequence(File)` method
- Keep `referenceSequence(Path)` method

**Interface**:
```java
public abstract class SamReaderFactory {
    // Primary methods
    public SamReader open(Path path);
    public SamReader open(URI uri) throws IOException;
    public SamReader open(SamInputResource resource);
    
    // Configuration
    public SamReaderFactory referenceSequence(Path referenceSequence);
    public SamReaderFactory referenceSequence(URI referenceUri) throws IOException;
    
    // Utilities
    public SAMFileHeader getFileHeader(Path samPath);
    public SAMFileHeader getFileHeader(URI samUri) throws IOException;
    
    // NO File-based methods
}
```

#### SAMFileWriterFactory

**Purpose**: Factory for creating SAM/BAM/CRAM writers.

**Key Changes**:
- Replace all File parameters with Path
- Update all writer creation methods

**Interface**:
```java
public class SAMFileWriterFactory {
    public SAMFileWriter makeWriter(SAMFileHeader header, boolean presorted, Path outputPath, Path referencePath);
    public SAMFileWriter makeBAMWriter(SAMFileHeader header, boolean presorted, Path outputPath);
    public SAMFileWriter makeSAMWriter(SAMFileHeader header, boolean presorted, Path outputPath);
    public SAMFileWriter makeCRAMWriter(SAMFileHeader header, Path outputPath, Path referencePath);
    
    // NO File-based methods
}
```

### Reader Components

All reader classes follow a similar pattern:

**Key Changes**:
- Update constructors to accept Path instead of File
- Remove File-based constructors
- Ensure internal operations use Path

**Example Interface Pattern**:
```java
public class BAMFileReader implements SamReader {
    // Path-based constructors
    public BAMFileReader(Path path, Path indexPath, boolean eagerDecode, ValidationStringency validationStringency);
    public BAMFileReader(Path path, Path indexPath, boolean eagerDecode);
    
    // NO File-based constructors
}
```

**Affected Reader Classes**:
- SAMTextReader
- BAMFileReader
- CRAMFileReader
- VCFFileReader
- FastqReader

### Writer Components

All writer classes follow a similar pattern:

**Key Changes**:
- Update constructors to accept Path instead of File
- Remove File-based constructors
- Ensure internal operations use Path

**Example Interface Pattern**:
```java
public class BAMFileWriter extends SAMFileWriterImpl {
    // Path-based constructors
    public BAMFileWriter(Path path, OutputStream os, SAMFileHeader header, boolean presorted, boolean writeHeader);
    
    // NO File-based constructors
}
```

**Affected Writer Classes**:
- SAMTextWriter
- BAMFileWriter
- CRAMFileWriter
- VCFWriter
- BCF2Writer
- BasicFastqWriter

### Index Components

#### Index Readers

**Key Changes**:
- Accept Path parameters for index files
- Support automatic index discovery using Path operations
- Support explicit index Path specification

**Example Interface Pattern**:
```java
public abstract class AbstractBAMFileIndex implements BAMIndex {
    protected AbstractBAMFileIndex(Path indexPath);
    
    public static BAMIndex openIndex(Path dataPath, Path indexPath);
    public static Path findIndexPath(Path dataPath);
    
    // NO File-based methods
}
```

**Affected Index Reader Classes**:
- AbstractBAMFileIndex
- DiskBasedBAMFileIndex
- CSIIndex
- SBIIndex
- Tribble index classes

#### Index Writers

**Key Changes**:
- Accept Path parameters for output
- Use Path for index file naming

**Example Interface Pattern**:
```java
public class BAMIndexer {
    public BAMIndexer(Path outputPath, SAMFileHeader header);
    
    public void createIndex(Path bamPath, Path indexPath);
    
    // NO File-based methods
}
```

**Affected Index Writer Classes**:
- BAMIndexer
- BinaryBAMIndexWriter
- CRAMBAIIndexer
- CRAMCRAIIndexer
- Index factory classes

### Reference Sequence Components

**Key Changes**:
- Accept Path parameters for FASTA files
- Accept Path parameters for FASTA index files
- Support URI-based reference access

**Example Interface Pattern**:
```java
public class IndexedFastaSequenceFile implements ReferenceSequenceFile {
    public IndexedFastaSequenceFile(Path path);
    public IndexedFastaSequenceFile(Path path, Path indexPath);
    public IndexedFastaSequenceFile(Path path, FastaSequenceIndex index);
    
    // NO File-based constructors
}
```

**Affected Classes**:
- FastaSequenceFile
- IndexedFastaSequenceFile
- FastaSequenceIndex
- AbstractFastaSequenceFile
- ReferenceSequenceFileFactory
- ReferenceSequenceFileWalker

### Utility Components

#### SamFiles

**Purpose**: Utilities for SAM/BAM/CRAM file operations.

**Key Changes**:
- All methods accept and return Path
- Index discovery uses Path operations

**Interface**:
```java
public class SamFiles {
    public static Path findIndex(Path dataPath);
    public static boolean isBAMFile(Path path);
    public static boolean isCRAMFile(Path path);
    public static boolean isSAMFile(Path path);
    
    // NO File-based methods
}
```

#### Tribble

**Purpose**: Utilities for Tribble index file naming.

**Key Changes**:
- Return Path instead of File
- Use Path operations for name construction

**Interface**:
```java
public class Tribble {
    public static Path indexPath(Path featurePath);
    public static Path tabixIndexPath(Path featurePath);
    
    // NO File-based methods
}
```

## Data Models

### Path Construction Patterns

**Pattern 1: From String**
```java
Path path = Path.of("/path/to/file.bam");
```

**Pattern 2: From URI (Local File)**
```java
URI uri = URI.create("file:///path/to/file.bam");
Path path = Path.of(uri);
```

**Pattern 3: From URI (Custom Filesystem)**
```java
URI uri = URI.create("s3://bucket/file.bam");
Path path = Path.of(uri);  // Requires S3 NIO SPI provider
```

**Pattern 4: Path Resolution**
```java
Path basePath = Path.of("/data");
Path filePath = basePath.resolve("file.bam");
```

**Pattern 5: Index Path Construction**
```java
Path dataPath = Path.of("/data/file.bam");
Path indexPath = Path.of(dataPath.toString() + ".bai");
```

### URI Schemes Supported

- **file://**: Local filesystem (default)
- **http://** and **https://**: Remote HTTP(S) access
- **ftp://**: FTP access
- **Custom schemes**: Any scheme supported by installed NIO SPI providers (s3://, hdfs://, etc.)

### Error Handling Patterns

**Pattern 1: Path Not Found**
```java
if (!Files.exists(path)) {
    throw new IOException("File not found: " + path.toAbsolutePath());
}
```

**Pattern 2: Permission Denied**
```java
if (!Files.isReadable(path)) {
    throw new IOException("Cannot read file: " + path.toAbsolutePath());
}
```

**Pattern 3: Malformed URI**
```java
try {
    Path path = Path.of(uri);
} catch (IllegalArgumentException e) {
    throw new IOException("Invalid URI: " + uri, e);
}
```

**Pattern 4: Missing NIO SPI Provider**
```java
try {
    Path path = Path.of(URI.create("s3://bucket/file.bam"));
} catch (FileSystemNotFoundException e) {
    throw new IOException("S3 filesystem provider not available", e);
}
```

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system—essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: URI to Path Round Trip

*For any* valid URI with an available filesystem provider, constructing a Path from that URI and then getting the URI back should preserve the original URI scheme and path components.

**Validates: Requirements 1.4, 9.4**

### Property 2: Path Resolution Consistency

*For any* base Path and relative path string, resolving the relative path should produce a Path that, when converted to a string, contains both the base and relative components in the correct order.

**Validates: Requirements 13.2**

### Property 3: Index Path Construction

*For any* data file Path, constructing the index Path by appending the appropriate extension should produce a Path in the same filesystem and parent directory as the data file.

**Validates: Requirements 14.5**

### Property 4: Index Auto-Discovery for BAM Files

*For any* BAM file Path where a corresponding .bai file exists at the standard location (same name + .bai extension), the index discovery mechanism should successfully locate the index Path.

**Validates: Requirements 14.1**

### Property 5: Index Auto-Discovery for CRAM Files

*For any* CRAM file Path where a corresponding .crai file exists at the standard location (same name + .crai extension), the index discovery mechanism should successfully locate the index Path.

**Validates: Requirements 14.2**

### Property 6: Index Auto-Discovery for VCF Files

*For any* VCF file Path where a corresponding .idx or .tbi file exists at the standard location, the index discovery mechanism should successfully locate the index Path.

**Validates: Requirements 14.3**

### Property 7: NIO SPI Compatibility with Jimfs

*For any* valid file created in a jimfs (in-memory) filesystem, HTSJDK readers and writers should be able to successfully open, read from, and write to that file using Path-based APIs.

**Validates: Requirements 9.3, 10.3**

### Property 8: NIO SPI Compatibility with Custom Filesystems

*For any* custom filesystem implementation that provides a valid NIO SPI provider, HTSJDK should be able to operate on Paths from that filesystem without modification.

**Validates: Requirements 9.3, 10.4, 14.6**

### Property 9: Error Messages Include Path Information

*For any* I/O error that occurs when accessing a Path, the exception message should contain the string representation of the Path that caused the error.

**Validates: Requirements 15.1, 15.5**

### Property 10: Error Messages Include URI Information

*For any* error that occurs when constructing a Path from a URI, the exception message should contain the string representation of the URI that caused the error.

**Validates: Requirements 15.3, 15.5**

## Error Handling

### Error Categories

#### 1. Path Not Found Errors

**Scenario**: Attempting to open a file that doesn't exist.

**Handling**:
- Check `Files.exists(path)` before opening
- Throw `IOException` with full path information
- Include absolute path in error message

**Example**:
```java
if (!Files.exists(path)) {
    throw new IOException("File not found: " + path.toAbsolutePath());
}
```

#### 2. Permission Errors

**Scenario**: Attempting to read/write a file without proper permissions.

**Handling**:
- Check `Files.isReadable(path)` or `Files.isWritable(path)`
- Throw `IOException` with permission details
- Include path and operation in error message

**Example**:
```java
if (!Files.isReadable(path)) {
    throw new IOException("Cannot read file (permission denied): " + path.toAbsolutePath());
}
```

#### 3. URI Parsing Errors

**Scenario**: Malformed URI provided to Path.of(URI).

**Handling**:
- Catch `IllegalArgumentException` from `Path.of(URI)`
- Wrap in `IOException` with URI details
- Provide guidance on correct URI format

**Example**:
```java
try {
    return Path.of(uri);
} catch (IllegalArgumentException e) {
    throw new IOException("Invalid URI: " + uri + ". Expected format: scheme://path", e);
}
```

#### 4. Missing Filesystem Provider Errors

**Scenario**: URI scheme has no registered NIO SPI provider.

**Handling**:
- Catch `FileSystemNotFoundException`
- Throw `IOException` indicating missing provider
- Suggest installing the appropriate provider

**Example**:
```java
try {
    return Path.of(uri);
} catch (FileSystemNotFoundException e) {
    throw new IOException("No filesystem provider for scheme: " + uri.getScheme() + 
                         ". Install the appropriate NIO SPI provider.", e);
}
```

#### 5. Index Not Found Errors

**Scenario**: Index file cannot be located automatically.

**Handling**:
- Return null from index discovery methods
- Allow explicit index Path specification
- Provide clear message when index is required but missing

**Example**:
```java
Path indexPath = findIndex(dataPath);
if (indexPath == null && indexRequired) {
    throw new IOException("Index file not found for: " + dataPath + 
                         ". Specify index path explicitly or create index.");
}
```

### Error Handling Principles

1. **Always include Path/URI in error messages**: Users need to know which file caused the problem
2. **Use absolute paths in errors**: Relative paths can be ambiguous
3. **Provide actionable guidance**: Tell users what they can do to fix the problem
4. **Preserve exception chains**: Use exception chaining to maintain context
5. **Fail fast**: Check preconditions early and throw exceptions immediately

## Testing Strategy

### Dual Testing Approach

The migration requires both unit tests and property-based tests to ensure comprehensive coverage:

- **Unit tests**: Verify specific examples, edge cases, and error conditions
- **Property tests**: Verify universal properties across all inputs using randomization

Both types of tests are complementary and necessary for comprehensive validation.

### Unit Testing

**Focus Areas**:
1. **API Surface Validation**: Verify that classes have Path-based methods and no File-based methods
2. **Specific Examples**: Test with known good files (e.g., test resources)
3. **Edge Cases**: Empty paths, special characters, very long paths
4. **Error Conditions**: Non-existent files, permission errors, malformed URIs
5. **Integration Points**: Factory → Reader → Index interactions

**Example Unit Tests**:
```java
@Test
public void testSamReaderFactoryAcceptsPath() {
    Path bamPath = Path.of("src/test/resources/htsjdk/samtools/example.bam");
    SamReader reader = SamReaderFactory.makeDefault().open(bamPath);
    assertNotNull(reader);
}

@Test
public void testNoFileBasedMethodsInIOUtil() {
    Method[] methods = IOUtil.class.getMethods();
    for (Method method : methods) {
        for (Class<?> paramType : method.getParameterTypes()) {
            assertNotEquals(File.class, paramType, 
                "IOUtil should not have File parameters: " + method.getName());
        }
        assertNotEquals(File.class, method.getReturnType(),
            "IOUtil should not return File: " + method.getName());
    }
}

@Test(expectedExceptions = IOException.class)
public void testNonExistentPathThrowsException() throws IOException {
    Path nonExistent = Path.of("/does/not/exist.bam");
    SamReaderFactory.makeDefault().open(nonExistent);
}
```

### Property-Based Testing

**Property Testing Library**: Use **jqwik** for Java property-based testing (minimum 100 iterations per test).

**Focus Areas**:
1. **URI Round-Trip**: URI → Path → URI preserves scheme and path
2. **Path Resolution**: Base + relative always produces valid combined path
3. **Index Discovery**: Index files at standard locations are always found
4. **NIO SPI Compatibility**: Operations work on any valid filesystem
5. **Error Messages**: All errors include path/URI information

**Example Property Tests**:
```java
@Property
// Feature: file-to-path-migration, Property 1: URI to Path Round Trip
void uriToPathRoundTrip(@ForAll("fileUris") URI uri) {
    Path path = Path.of(uri);
    URI resultUri = path.toUri();
    assertEquals(uri.getScheme(), resultUri.getScheme());
    assertEquals(uri.getPath(), resultUri.getPath());
}

@Property
// Feature: file-to-path-migration, Property 2: Path Resolution Consistency
void pathResolutionConsistency(
        @ForAll("absolutePaths") Path basePath,
        @ForAll("relativePaths") String relativePath) {
    Path resolved = basePath.resolve(relativePath);
    String resolvedString = resolved.toString();
    assertTrue(resolvedString.contains(basePath.getFileName().toString()));
    assertTrue(resolvedString.contains(relativePath));
}

@Property
// Feature: file-to-path-migration, Property 3: Index Path Construction
void indexPathConstruction(@ForAll("dataFilePaths") Path dataPath) {
    Path indexPath = Path.of(dataPath.toString() + ".bai");
    assertEquals(dataPath.getParent(), indexPath.getParent());
    assertTrue(indexPath.getFileName().toString().startsWith(
        dataPath.getFileName().toString()));
}

@Property
// Feature: file-to-path-migration, Property 7: NIO SPI Compatibility with Jimfs
void jimfsCompatibility(@ForAll("bamFileContents") byte[] bamData) throws IOException {
    try (FileSystem fs = Jimfs.newFileSystem(Configuration.unix())) {
        Path testFile = fs.getPath("/test.bam");
        Files.write(testFile, bamData);
        
        // Should be able to open with HTSJDK
        SamReader reader = SamReaderFactory.makeDefault().open(testFile);
        assertNotNull(reader);
        reader.close();
    }
}

@Property
// Feature: file-to-path-migration, Property 9: Error Messages Include Path Information
void errorMessagesIncludePath(@ForAll("nonExistentPaths") Path path) {
    try {
        SamReaderFactory.makeDefault().open(path);
        fail("Should have thrown IOException");
    } catch (IOException e) {
        assertTrue(e.getMessage().contains(path.toString()),
            "Error message should contain path: " + e.getMessage());
    }
}
```

**Generators for Property Tests**:
```java
@Provide
Arbitrary<URI> fileUris() {
    return Arbitraries.strings()
        .alpha().numeric().ofMinLength(1).ofMaxLength(50)
        .map(s -> URI.create("file:///" + s + ".bam"));
}

@Provide
Arbitrary<Path> absolutePaths() {
    return Arbitraries.strings()
        .alpha().numeric().ofMinLength(1).ofMaxLength(20)
        .map(s -> Path.of("/base/" + s));
}

@Provide
Arbitrary<String> relativePaths() {
    return Arbitraries.strings()
        .alpha().numeric().ofMinLength(1).ofMaxLength(20)
        .map(s -> s + ".bam");
}
```

### NIO SPI Testing

**Test with Multiple Filesystems**:
1. **Default filesystem**: Standard local filesystem
2. **Jimfs**: In-memory filesystem for fast, isolated tests
3. **Mock custom filesystem**: Verify NIO SPI contract compliance

**Example NIO SPI Test**:
```java
@Test
public void testReadWriteWithJimfs() throws IOException {
    try (FileSystem fs = Jimfs.newFileSystem(Configuration.unix())) {
        // Create test file in memory
        Path testBam = fs.getPath("/test.bam");
        Path testBai = fs.getPath("/test.bam.bai");
        
        // Copy real test data to jimfs
        Files.copy(
            Path.of("src/test/resources/htsjdk/samtools/example.bam"),
            testBam
        );
        Files.copy(
            Path.of("src/test/resources/htsjdk/samtools/example.bam.bai"),
            testBai
        );
        
        // Open with HTSJDK - should work transparently
        SamReader reader = SamReaderFactory.makeDefault().open(testBam);
        assertNotNull(reader);
        assertTrue(reader.hasIndex());
        
        // Read some records
        int count = 0;
        for (SAMRecord record : reader) {
            count++;
            if (count >= 10) break;
        }
        assertEquals(10, count);
        
        reader.close();
    }
}
```

### Test Coverage Goals

- **Line coverage**: > 80% for migrated code
- **Branch coverage**: > 75% for migrated code
- **Property test iterations**: Minimum 100 per property
- **NIO SPI coverage**: Test with at least 2 different filesystem implementations

### Regression Testing

**Ensure No Functional Changes**:
1. All existing tests must pass after migration (with Path instead of File)
2. File format reading/writing must produce identical results
3. Index creation must produce byte-identical index files
4. Performance must not regress significantly (< 5% slowdown acceptable)

**Regression Test Strategy**:
```java
@Test
public void testBamReadingProducesSameResults() throws IOException {
    // Read with old File-based API (before migration)
    List<SAMRecord> oldResults = readWithFileAPI();
    
    // Read with new Path-based API (after migration)
    List<SAMRecord> newResults = readWithPathAPI();
    
    // Results should be identical
    assertEquals(oldResults.size(), newResults.size());
    for (int i = 0; i < oldResults.size(); i++) {
        assertEquals(oldResults.get(i), newResults.get(i));
    }
}
```

### Test Execution

**Run tests with**:
```bash
./gradlew test                    # Run main test suite
./gradlew test --tests "*Path*"   # Run Path-related tests
./gradlew test --tests "*NIO*"    # Run NIO SPI tests
```

**Property tests configuration**:
- Minimum 100 iterations per property (configured in jqwik.properties)
- Shrinking enabled to find minimal failing examples
- Seed recording for reproducibility

### Test Documentation

Each property test must include:
1. **Feature tag**: `// Feature: file-to-path-migration`
2. **Property reference**: `// Property N: <property title>`
3. **Requirements validation**: `// Validates: Requirements X.Y`
4. **Clear assertion messages**: Explain what is being tested

This ensures traceability from requirements → design properties → test implementation.
