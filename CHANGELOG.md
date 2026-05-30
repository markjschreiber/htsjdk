# Changelog

All notable changes to HTSJDK will be documented in this file.

## [4.0.0] - Unreleased

### Breaking Changes

This is a major release that removes all `java.io.File`-based APIs in favor of `java.nio.file.Path`.
See the [Migration Guide](MIGRATION_GUIDE_4.0.md) for detailed upgrade instructions.

#### IOUtil (`htsjdk.samtools.util.IOUtil`)
- All `File`-based methods removed (assertFileIsReadable, openFileForReading, openFileForWriting,
  deleteFiles, getDefaultTmpDir, etc.)
- Use the equivalent `Path`-based methods instead

#### SamReaderFactory (`htsjdk.samtools.SamReaderFactory`)
- `open(File)` removed — use `open(Path)` or `open(URI)`
- All `File`-based overloads removed from the factory

#### SAMFileWriterFactory (`htsjdk.samtools.SAMFileWriterFactory`)
- All `File`-based writer creation methods removed (makeBAMWriter, makeSAMWriter,
  makeSAMOrBAMWriter, makeCRAMWriter)
- `setTempDirectory(File)` removed — use `setTempDirectory(Path)`
- Use the equivalent `Path`-based methods instead

#### ReferenceSequenceFileFactory (`htsjdk.samtools.reference.ReferenceSequenceFileFactory`)
- `getReferenceSequenceFile(File)` and overloads removed
- Use `getReferenceSequenceFile(Path)` or `getReferenceSequenceFile(URI)` instead

#### Defaults (`htsjdk.samtools.Defaults`)
- `REFERENCE_FASTA` field type changed from `File` to `Path`

#### BAMIndexer (`htsjdk.samtools.BAMIndexer`)
- `BAMIndexer(File, SAMFileHeader)` constructor removed — use `BAMIndexer(Path, SAMFileHeader)`
- `createIndex(SamReader, File)` removed — use `createIndex(SamReader, Path)`

#### SamFiles (`htsjdk.samtools.SamFiles`)
- `findIndex(File)` removed — use `findIndex(Path)`

#### VCFFileReader (`htsjdk.variant.vcf.VCFFileReader`)
- All `File`-based constructors removed — use `Path`-based constructors

#### AbstractFeatureReader (`htsjdk.tribble.AbstractFeatureReader`)
- `File`-based factory methods removed — use `Path`-based equivalents

#### Test Utilities
- `TestUtil` methods changed to use `Path` instead of `File`

### Added
- Full NIO SPI compatibility — custom filesystem providers (jimfs, S3, HDFS, GCS) work
  transparently with all HTSJDK APIs
- `SamReaderFactory.open(URI)` for direct URI-based access
- `ReferenceSequenceFileFactory.getReferenceSequenceFile(URI)` for URI-based reference access

### Changed
- Minimum Java version is now 17
- All internal I/O operations use `java.nio.file.Path` and `java.nio.file.Files`

### Migration
- See [MIGRATION_GUIDE_4.0.md](MIGRATION_GUIDE_4.0.md) for complete migration instructions,
  before/after code examples, and common patterns
