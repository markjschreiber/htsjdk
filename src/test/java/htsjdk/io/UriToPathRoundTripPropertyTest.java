package htsjdk.io;

import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

import java.net.URI;
import java.nio.file.Path;

/**
 * Property-based tests for URI to Path round trip conversion.
 * 
 * Feature: file-to-path-migration
 * Property 1: URI to Path Round Trip
 * Validates: Requirements 1.4, 9.4
 * 
 * For any valid URI with an available filesystem provider, constructing a Path 
 * from that URI and then getting the URI back should preserve the original URI 
 * scheme and path components.
 */
public class UriToPathRoundTripPropertyTest {

    /**
     * Property 1: URI to Path Round Trip
     * 
     * For any valid file:// URI, Path.of(URI) → toUri() preserves scheme and path.
     * 
     * Validates: Requirements 1.4, 9.4
     */
    @Property(tries = 100)
    void uriToPathRoundTripPreservesSchemeAndPath(
            @ForAll("validFileUris") URI originalUri) {
        
        // Convert URI to Path
        Path path = Path.of(originalUri);
        
        // Convert Path back to URI
        URI resultUri = path.toUri();
        
        // Verify scheme is preserved
        assert originalUri.getScheme().equals(resultUri.getScheme()) :
                String.format("Scheme mismatch: original=%s, result=%s", 
                        originalUri.getScheme(), resultUri.getScheme());
        
        // Verify path component is preserved (normalized)
        // Note: Path normalization may add trailing slashes or resolve relative components
        String originalPath = originalUri.getPath();
        String resultPath = resultUri.getPath();
        
        // The result path should contain the essential parts of the original path
        // After normalization, paths should be equivalent
        Path originalAsPath = Path.of(originalUri);
        Path resultAsPath = Path.of(resultUri);
        
        assert originalAsPath.equals(resultAsPath) :
                String.format("Path mismatch after round trip: original=%s, result=%s",
                        originalAsPath, resultAsPath);
    }

    /**
     * Property 1 (variant): URI string round trip through HtsPath
     * 
     * For any valid file path string, creating an HtsPath and getting the URI
     * should produce a valid URI that can be converted back to an equivalent Path.
     * 
     * Validates: Requirements 1.4, 9.4
     */
    @Property(tries = 100)
    void htsPathUriRoundTrip(
            @ForAll("validFilePathStrings") String pathString) {
        
        // Create HtsPath from string
        HtsPath htsPath = new HtsPath(pathString);
        
        // Get URI from HtsPath
        URI uri = htsPath.getURI();
        
        // Verify URI has file scheme
        assert "file".equals(uri.getScheme()) :
                String.format("Expected file scheme, got: %s", uri.getScheme());
        
        // Verify the path can be converted back
        if (htsPath.isPath()) {
            Path path = htsPath.toPath();
            URI pathUri = path.toUri();
            
            // Both URIs should have the same scheme
            assert uri.getScheme().equals(pathUri.getScheme()) :
                    String.format("Scheme mismatch: htsPath URI=%s, path URI=%s",
                            uri.getScheme(), pathUri.getScheme());
            
            // The paths should be equivalent after normalization
            Path fromHtsPathUri = Path.of(uri);
            assert fromHtsPathUri.equals(path) :
                    String.format("Path mismatch: from HtsPath URI=%s, direct path=%s",
                            fromHtsPathUri, path);
        }
    }

    /**
     * Provides valid file:// URIs for testing.
     * Generates URIs with the file scheme and valid path components.
     */
    @Provide
    Arbitrary<URI> validFileUris() {
        return validPathComponents()
                .map(pathComponent -> {
                    // Construct a valid file:// URI
                    String uriString = "file:///" + pathComponent;
                    return URI.create(uriString);
                });
    }

    /**
     * Provides valid file path strings (without scheme).
     * These will be interpreted as local file references.
     */
    @Provide
    Arbitrary<String> validFilePathStrings() {
        return validPathComponents()
                .map(component -> "/" + component);
    }

    /**
     * Generates valid path components for URIs.
     * Avoids characters that are problematic in URIs or file paths.
     */
    @Provide
    Arbitrary<String> validPathComponents() {
        // Generate alphanumeric strings with some allowed special characters
        Arbitrary<String> dirNames = Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(10);
        
        Arbitrary<String> fileNames = Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(10);
        
        Arbitrary<String> extensions = Arbitraries.of("bam", "sam", "vcf", "txt", "fasta", "cram");
        
        // Combine to create path like "dir1/dir2/filename.ext"
        return Combinators.combine(
                Arbitraries.integers().between(0, 3),
                dirNames.list().ofMinSize(0).ofMaxSize(3),
                fileNames,
                extensions
        ).as((depth, dirs, fileName, ext) -> {
            StringBuilder path = new StringBuilder();
            for (int i = 0; i < Math.min(depth, dirs.size()); i++) {
                if (path.length() > 0) {
                    path.append("/");
                }
                path.append(dirs.get(i));
            }
            if (path.length() > 0) {
                path.append("/");
            }
            path.append(fileName).append(".").append(ext);
            return path.toString();
        });
    }
}
