# Project Structure

## Source Organization

```
src/
├── main/java/htsjdk/          # Main source code
│   ├── annotations/           # API stability annotations (@BetaAPI, @InternalAPI)
│   ├── beta/                  # Beta/experimental features
│   │   ├── codecs/           # New codec implementations
│   │   ├── io/               # I/O abstractions
│   │   └── plugin/           # Plugin system
│   ├── io/                    # Core I/O (HtsPath, IOPath, Writer)
│   ├── samtools/              # SAM/BAM/CRAM format support
│   │   ├── cram/             # CRAM-specific code (unstable API)
│   │   ├── fastq/            # FASTQ format
│   │   ├── filter/           # Read filtering
│   │   ├── liftover/         # Coordinate conversion
│   │   ├── metrics/          # Quality metrics
│   │   ├── reference/        # Reference sequence handling
│   │   ├── seekablestream/   # Stream abstractions
│   │   ├── sra/              # SRA format support
│   │   └── util/             # Utilities
│   ├── tribble/               # Feature file framework (VCF, BED, GFF)
│   │   ├── bed/              # BED format
│   │   ├── gff/              # GFF format
│   │   ├── index/            # Indexing
│   │   └── readers/          # File readers
│   ├── utils/                 # General utilities
│   └── variant/               # Variant calling formats
│       ├── bcf2/             # BCF format
│       ├── variantcontext/   # Variant representation
│       └── vcf/              # VCF format
│
├── main/resources/META-INF/   # Service provider configs
│
└── test/                      # Test code mirrors main structure
    ├── java/htsjdk/          # Test classes
    ├── resources/htsjdk/     # Test data files
    └── scala/htsjdk/         # Scala test utilities
```

## Key Packages

### htsjdk.samtools
Core package for SAM/BAM/CRAM alignment formats. Contains readers, writers, indexers, and record representations.

**Important classes:**
- `SAMRecord` - Represents a single alignment
- `SAMFileHeader` - File header with metadata
- `SamReader`/`SamReaderFactory` - Reading alignments
- `SAMFileWriter`/`SAMFileWriterFactory` - Writing alignments
- `BAMFileReader`, `CRAMFileReader` - Format-specific readers
- `Cigar`, `CigarElement` - Alignment representation

### htsjdk.tribble
Framework for genomic feature files (VCF, BED, GFF). Provides codec pattern for reading/writing.

**Important classes:**
- `FeatureReader`, `AbstractFeatureReader` - Reading features
- `FeatureCodec` - Codec interface
- `TabixFeatureReader` - Tabix-indexed files

### htsjdk.variant
Variant calling format (VCF/BCF) support built on Tribble.

**Important classes:**
- `VariantContext` - Represents a variant
- `VCFFileReader`, `VCFFileWriter` - VCF I/O
- `VCFHeader` - VCF header

### htsjdk.beta
Experimental features with unstable APIs. Subject to change without notice.

## Naming Conventions

- **Classes**: PascalCase (e.g., `SAMRecord`, `BAMFileReader`)
- **Interfaces**: PascalCase, often descriptive (e.g., `FeatureCodec`, `SamReader`)
- **Constants**: UPPER_SNAKE_CASE (e.g., `NO_ALIGNMENT_REFERENCE_NAME`)
- **Methods/Variables**: camelCase (e.g., `getReadName()`, `mReadBases`)
- **Member fields**: Prefix with `m` (e.g., `mReadName`, `mAlignmentStart`)
- **Test classes**: Suffix with `Test` (e.g., `SAMRecordTest`, `BAMFileReaderTest`)

## Code Style

- **Indentation**: 4 spaces (no tabs)
- **Line length**: 100 characters (soft limit)
- **Braces**: End-of-line style (K&R)
- **Style guides**: `java-style-eclipse.xml` and `java-style-intellij.xml` provided
- Based on Google Java Style with modifications
- Style is suggested for new code but not rigidly enforced
- Match surrounding code style when modifying existing files

## API Stability

- **@BetaAPI**: Experimental, may change
- **@InternalAPI**: Internal use only
- **htsjdk.samtools.cram**: Unstable, undergoing major changes
- Public APIs are generally stable; breaking changes documented in release notes
- Protected members may change more freely than public ones

## Test Organization

- Test classes extend `HtsjdkTest` base class
- Use TestNG `@Test` annotation (not JUnit)
- Data providers for parameterized tests: `@Test(dataProvider = "name")`
- Test groups for categorization: `@Test(groups = {"slow", "ftp", etc.})`
- Test resources organized by package under `src/test/resources/htsjdk/`
