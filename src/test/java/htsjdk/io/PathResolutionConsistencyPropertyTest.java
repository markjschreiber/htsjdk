package htsjdk.io;

import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

import java.nio.file.Path;

/**
 * Property-based tests for Path resolution consistency.
 * 
 * Feature: file-to-path-migration
 * Property 2: Path Resolution Consistency
 * Validates: Requirements 13.2
 * 
 * For any base Path and relative path string, resolving the relative path 
 * should produce a Path that, when converted to a string, contains both 
 * the base and relative components in the correct order.
 */
public class PathResolutionConsistencyPropertyTest {

    /**
     * Property 2: Path Resolution Consistency
     * 
     * For any base Path and relative path string, resolving the relative path
     * using Path.resolve() should produce a Path that contains both the base
     * and relative components.
     * 
     * Validates: Requirements 13.2
     */
    @Property(tries = 100)
    void pathResolutionContainsBothComponents(
            @ForAll("absoluteBasePaths") Path basePath,
            @ForAll("relativePathStrings") String relativePath) {
        
        // Resolve the relative path against the base path
        Path resolved = basePath.resolve(relativePath);
        
        // Convert resolved path to string for verification
        String resolvedString = resolved.toString();
        
        // The resolved path should contain the base path's directory structure
        // Note: We check that the base path's parent directories are present
        Path baseParent = basePath.getParent();
        if (baseParent != null) {
            String baseParentString = baseParent.toString();
            assert resolvedString.startsWith(baseParentString) :
                    String.format("Resolved path should start with base parent. " +
                            "Base parent: %s, Resolved: %s", baseParentString, resolvedString);
        }
        
        // The resolved path should end with the relative path component
        assert resolvedString.endsWith(relativePath) :
                String.format("Resolved path should end with relative path. " +
                        "Relative: %s, Resolved: %s", relativePath, resolvedString);
        
        // The resolved path should be longer than or equal to both components
        // (accounting for path separator)
        assert resolved.getNameCount() >= basePath.getNameCount() :
                String.format("Resolved path should have at least as many components as base. " +
                        "Base count: %d, Resolved count: %d", 
                        basePath.getNameCount(), resolved.getNameCount());
    }

    /**
     * Property 2 (variant): Path resolution preserves absolute nature
     * 
     * When resolving a relative path against an absolute base path,
     * the result should also be absolute.
     * 
     * Validates: Requirements 13.2
     */
    @Property(tries = 100)
    void pathResolutionPreservesAbsoluteNature(
            @ForAll("absoluteBasePaths") Path basePath,
            @ForAll("relativePathStrings") String relativePath) {
        
        // Resolve the relative path against the base path
        Path resolved = basePath.resolve(relativePath);
        
        // If base path is absolute, resolved path should also be absolute
        assert basePath.isAbsolute() == resolved.isAbsolute() :
                String.format("Resolved path should preserve absolute nature. " +
                        "Base absolute: %b, Resolved absolute: %b",
                        basePath.isAbsolute(), resolved.isAbsolute());
    }

    /**
     * Property 2 (variant): Path resolution is consistent with getParent/getFileName
     * 
     * The resolved path's parent should be related to the base path,
     * and the filename should match the relative path's filename.
     * 
     * Validates: Requirements 13.2
     */
    @Property(tries = 100)
    void pathResolutionConsistentWithParentAndFileName(
            @ForAll("absoluteBasePaths") Path basePath,
            @ForAll("simpleFileNames") String fileName) {
        
        // Resolve a simple filename against the base path
        Path resolved = basePath.resolve(fileName);
        
        // The resolved path's parent should be the base path
        Path resolvedParent = resolved.getParent();
        assert basePath.equals(resolvedParent) :
                String.format("Resolved path's parent should equal base path. " +
                        "Base: %s, Resolved parent: %s", basePath, resolvedParent);
        
        // The resolved path's filename should match the input filename
        Path resolvedFileName = resolved.getFileName();
        assert resolvedFileName != null && resolvedFileName.toString().equals(fileName) :
                String.format("Resolved path's filename should match input. " +
                        "Input: %s, Resolved filename: %s", fileName, resolvedFileName);
    }

    /**
     * Property 2 (variant): Nested path resolution is associative
     * 
     * Resolving path1 then path2 should equal resolving (path1/path2) directly.
     * 
     * Validates: Requirements 13.2
     */
    @Property(tries = 100)
    void nestedPathResolutionIsAssociative(
            @ForAll("absoluteBasePaths") Path basePath,
            @ForAll("directoryNames") String dir,
            @ForAll("simpleFileNames") String fileName) {
        
        // Method 1: Resolve in two steps
        Path step1 = basePath.resolve(dir);
        Path step2 = step1.resolve(fileName);
        
        // Method 2: Resolve combined path in one step
        String combinedRelative = dir + "/" + fileName;
        Path combined = basePath.resolve(combinedRelative);
        
        // Both methods should produce equivalent paths
        assert step2.equals(combined) :
                String.format("Nested resolution should be associative. " +
                        "Two-step: %s, Combined: %s", step2, combined);
    }

    /**
     * Provides absolute base paths for testing.
     * Generates paths like /base/dir1/dir2
     */
    @Provide
    Arbitrary<Path> absoluteBasePaths() {
        return Combinators.combine(
                directoryNames().list().ofMinSize(1).ofMaxSize(4),
                Arbitraries.integers().between(1, 4)
        ).as((dirs, depth) -> {
            StringBuilder pathStr = new StringBuilder("/");
            int actualDepth = Math.min(depth, dirs.size());
            for (int i = 0; i < actualDepth; i++) {
                if (i > 0) {
                    pathStr.append("/");
                }
                pathStr.append(dirs.get(i));
            }
            return Path.of(pathStr.toString());
        });
    }

    /**
     * Provides relative path strings for testing.
     * Generates paths like "subdir/file.bam" or just "file.txt"
     */
    @Provide
    Arbitrary<String> relativePathStrings() {
        return Combinators.combine(
                directoryNames().list().ofMinSize(0).ofMaxSize(2),
                simpleFileNames()
        ).as((dirs, fileName) -> {
            StringBuilder pathStr = new StringBuilder();
            for (String dir : dirs) {
                pathStr.append(dir).append("/");
            }
            pathStr.append(fileName);
            return pathStr.toString();
        });
    }

    /**
     * Provides simple file names for testing.
     * Generates names like "file.bam", "data.vcf"
     */
    @Provide
    Arbitrary<String> simpleFileNames() {
        Arbitrary<String> baseName = Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(10);
        
        Arbitrary<String> extension = Arbitraries.of(
                "bam", "sam", "vcf", "txt", "fasta", "cram", "bed", "gff");
        
        return Combinators.combine(baseName, extension)
                .as((name, ext) -> name + "." + ext);
    }

    /**
     * Provides directory names for testing.
     * Generates simple alphanumeric directory names.
     */
    @Provide
    Arbitrary<String> directoryNames() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(10);
    }
}
