# HTSJDK File to Path Migration Plan

## Overview
This document outlines the plan to migrate HTSJDK from `java.io.File` to `java.nio.file.Path` with URI constructors for full Java NIO SPI compatibility. This is a **breaking change** suitable for a new major version.

## Scope Analysis

Based on code analysis, `java.io.File` is used extensively throughout the codebase:
- **~50+ source files** in `src/main/java` import `java.io.File`
- **Hundreds of method signatures** accept or return `File` objects
- **Test files** mirror this usage pattern

### Key Affected Areas

1. **Core I/O Classes**
   - `htsjdk.io.HtsPath` and `htsjdk.io.IOPath` (already Path-aware)
   - `htsjdk.samtools.util.IOUtil` (utility methods)

2. **SAM/BAM/CRAM**
   - `SamReaderFactory`, `SAMFileWriterFactory`
   - `BAMIndexer`, `CRAMBAIIndexer`
   - Index readers/writers (`AbstractBAMFileIndex`, `CSIIndex`, etc.)
   - `SAMTextReader`, `SAMTextWriter`

3. **Reference Sequences**
   - `FastaSequenceFile`, `IndexedFastaSequenceFile`
   - `ReferenceSequenceFileFactory`, `ReferenceSequenceFileWalker`
   - `FastaSequenceIndex`, `AbstractFastaSequenceFile`

4. **VCF/BCF**
   - `VCFFileReader`, `VCFWriter`, `BCF2Writer`
   - `VariantContextWriterBuilder`
   - `IndexingVariantContextWriter`

5. **Tribble (Feature Files)**
   - `AbstractIndex`, `IndexFactory`
   - `DynamicIndexCreator`, `LinearIndex`
   - `Tribble` utility class

6. **FASTQ**
   - `FastqReader`, `FastqWriterFactory`, `BasicFastqWriter`

7. **Filters**
   - `JavascriptSamRecordFilter`, `JavascriptVariantFilter`
   - `ReadNameFilter`

8. **Utilities**
   - `SamFiles`, `BamFileIoUtils`
   - `SAMRecordSetBuilder`
   - `ClassFinder`

## Migration Strategy

### Phase 1: Preparation (Week 1-2)

1. **Create Migration Branch**
   ```bash
   git checkout -b feature/file-to-path-migration
   ```

2. **Update Version Number**
   - Update `build.gradle` to next major version (e.g., 4.0.0)
   - Document breaking changes in CHANGELOG

3. **Audit Existing Path Support**
   - Many classes already have Path-based methods
   - Identify which File methods can be deprecated vs removed

### Phase 2: Core Utilities (Week 2-3)

1. **Update `htsjdk.samtools.util.IOUtil`**
   - Remove File-based utility methods or mark as deprecated
   - Ensure all Path-based equivalents exist
   - Add URI-based Path construction helpers

2. **Update `htsjdk.io` Package**
   - Ensure `HtsPath` and `IOPath` are fully Path-based
   - Remove any File dependencies

### Phase 3: Factory Classes (Week 3-4)

Update factory classes to accept Path with URI constructors:

1. **`SamReaderFactory`**
   - Replace `open(File)` with `open(Path)` or `open(URI)`
   - Update internal File usage

2. **`SAMFileWriterFactory`**
   - Replace File parameters with Path
   - Update all writer creation methods

3. **`ReferenceSequenceFileFactory`**
   - Migrate to Path-based factory methods

4. **`VariantContextWriterBuilder`**
   - Replace `setOutputFile(File)` with Path equivalent

### Phase 4: Reader/Writer Classes (Week 4-6)

Systematically update each reader/writer:

1. **SAM/BAM/CRAM Readers**
   - `SAMTextReader`, `BAMFileReader`, `CRAMFileReader`
   - Update constructors and methods

2. **SAM/BAM/CRAM Writers**
   - `SAMTextWriter`, `BAMFileWriter`, `CRAMFileWriter`
   - Update constructors and methods

3. **VCF/BCF**
   - `VCFFileReader`, `VCFWriter`, `BCF2Writer`
   - Update all File-based APIs

4. **FASTQ**
   - `FastqReader`, `BasicFastqWriter`

### Phase 5: Index Classes (Week 6-7)

1. **Index Readers**
   - `AbstractBAMFileIndex`, `DiskBasedBAMFileIndex`
   - `CSIIndex`, `SBIIndex`
   - Tribble indexes

2. **Index Writers**
   - `BAMIndexer`, `BinaryBAMIndexWriter`
   - `CRAMBAIIndexer`, `CRAMCRAIIndexer`
   - Index factory classes

### Phase 6: Reference Sequences (Week 7-8)

1. **`FastaSequenceFile` and subclasses**
2. **`FastaSequenceIndex`**
3. **`ReferenceSequenceFileWalker`**

### Phase 7: Utility Classes (Week 8-9)

1. **`SamFiles`** - Path-based file utilities
2. **`BamFileIoUtils`** - BAM file operations
3. **`Tribble`** - Index file naming
4. **Filter classes** - JavaScript filters, read name filters

### Phase 8: Test Migration (Week 9-11)

1. **Update Test Utilities**
   - `SAMRecordSetBuilder`
   - `TestUtil` classes

2. **Migrate All Tests**
   - Update test data file access
   - Replace `File` with `Path` in test methods
   - Use `Path.of(URI)` for test resources

3. **Add NIO SPI Tests**
   - Test with custom FileSystem implementations
   - Verify URI-based Path construction works
   - Test with jimfs (in-memory filesystem)

### Phase 9: Documentation & Examples (Week 11-12)

1. **Update JavaDoc**
   - Document breaking changes
   - Provide migration examples

2. **Update Examples**
   - `PrintVariantsExample` and other example classes

3. **Create Migration Guide**
   - Document common migration patterns
   - Provide before/after code examples

4. **Update README**
   - Note breaking changes
   - Update version compatibility matrix

## Implementation Guidelines

### URI-Based Path Construction

**Preferred Pattern:**
```java
// For local files
Path path = Path.of(URI.create("file:///path/to/file.bam"));

// For custom filesystems
Path path = Path.of(URI.create("s3://bucket/file.bam"));

// From string (when URI not needed)
Path path = Path.of("/path/to/file.bam");
```

### Method Signature Changes

**Before:**
```java
public SamReader open(File file) throws IOException
public void write(File outputFile) throws IOException
```

**After:**
```java
public SamReader open(Path path) throws IOException
public void write(Path outputPath) throws IOException

// Optional: Add URI-based convenience methods
public SamReader open(URI uri) throws IOException {
    return open(Path.of(uri));
}
```

### Backward Compatibility Considerations

Since this is a **major version with breaking changes**, we will:

1. **Remove deprecated File-based methods** that have Path equivalents
2. **Not provide File-based overloads** for new APIs
3. **Document migration path** clearly

However, consider:
- Keeping a few high-level File-based convenience methods if they're heavily used
- Providing clear error messages when File APIs are removed

### Testing Strategy

1. **Unit Tests**
   - Update all existing tests to use Path
   - Ensure test resources use URI-based paths

2. **Integration Tests**
   - Test with real filesystems
   - Test with jimfs (in-memory)
   - Test with custom NIO SPI implementations

3. **Compatibility Tests**
   - Verify all file formats still work
   - Test index creation and reading
   - Validate round-trip operations

## Breaking Changes Checklist

Document all breaking changes:

- [ ] List all removed File-based methods
- [ ] List all changed method signatures
- [ ] Document constructor changes
- [ ] Note any behavioral changes
- [ ] Provide migration examples for each

## Risk Mitigation

1. **Extensive Testing**
   - Run full test suite after each phase
   - Add new tests for Path/URI functionality

2. **Incremental Commits**
   - Commit after each logical unit of work
   - Keep commits focused and reviewable

3. **Code Review**
   - Review each phase before proceeding
   - Ensure consistency across codebase

4. **Beta Release**
   - Consider alpha/beta releases for early adopters
   - Gather feedback before final release

## Timeline Estimate

- **Total Duration**: 12 weeks (3 months)
- **Full-time effort**: 1-2 developers
- **Testing**: 25% of total time

## Success Criteria

- [ ] Zero `import java.io.File` in main source code
- [ ] All tests pass with Path-based APIs
- [ ] Successfully tested with custom NIO SPI
- [ ] Documentation complete
- [ ] Migration guide published
- [ ] All breaking changes documented

## Post-Migration

1. **Monitor Issues**
   - Track migration-related bug reports
   - Provide support for downstream projects

2. **Update Dependent Projects**
   - Notify major downstream consumers
   - Provide migration assistance

3. **Performance Validation**
   - Benchmark before/after
   - Ensure no performance regressions
