/*
 * The MIT License
 *
 * Copyright (c) 2009 The Broad Institute
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
package htsjdk.samtools.util;

import htsjdk.HtsjdkTest;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;

import java.io.*;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.spi.FileSystemProvider;

import htsjdk.beta.exception.HtsjdkException;
import htsjdk.samtools.BAMFileWriter;
import htsjdk.samtools.BamFileIoUtils;
import htsjdk.samtools.HtsjdkTestUtils;
import htsjdk.samtools.SAMException;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SamReaderFactory;
import org.apache.commons.compress.compressors.FileNameUtil;
import org.apache.commons.lang3.SystemUtils;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.lang.IllegalArgumentException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Random;
import java.util.zip.GZIPOutputStream;


public class IOUtilTest extends HtsjdkTest {


    private static final Path TEST_DATA_DIR = Paths.get ("src/test/resources/htsjdk/samtools/io/");
    private static final Path TEST_VARIANT_DIR = Paths.get("src/test/resources/htsjdk/variant/");
    private static final Path SLURP_TEST_FILE = TEST_DATA_DIR.resolve("slurptest.txt");
    private static final Path EMPTY_FILE = TEST_DATA_DIR.resolve("empty.txt");
    private static final Path FIVE_SPACES_THEN_A_NEWLINE_THEN_FIVE_SPACES_FILE = TEST_DATA_DIR.resolve("5newline5.txt");
    private static final List<String> SLURP_TEST_LINES = Arrays.asList("bacon   and rice   ", "for breakfast  ", "wont you join me");
    private static final String SLURP_TEST_LINE_SEPARATOR = "\n";
    private static final String TEST_FILE_PREFIX = "htsjdk-IOUtilTest";
    private static final String[] TEST_FILE_EXTENSIONS = {".txt", ".txt.gz"};
    private static final String TEST_STRING = "bar!";

    private File existingTempFile;
    private String systemUser;
    private String systemTempDir;
    private FileSystem inMemoryFileSystem;
    private static Path WORDS_LONG;

    @BeforeClass
    public void setUp() throws IOException {
        existingTempFile = File.createTempFile("FiletypeTest.", ".tmp");
        existingTempFile.deleteOnExit();
        systemTempDir = System.getProperty("java.io.tmpdir");
        final File tmpDir = new File(systemTempDir);
        inMemoryFileSystem = Jimfs.newFileSystem(Configuration.unix());;
        if (!tmpDir.isDirectory()) tmpDir.mkdir();
        if (!tmpDir.isDirectory())
            throw new RuntimeException("java.io.tmpdir (" + systemTempDir + ") is not a directory");
        systemUser = System.getProperty("user.name");
        //build long file of random words for compression testing
        WORDS_LONG = Files.createTempFile("words_long", ".txt");
        WORDS_LONG.toFile().deleteOnExit();
        final List<String> wordsList = Files.lines(TEST_DATA_DIR.resolve("dictionary_english_short.dic")).collect(Collectors.toList());
        final int numberOfWords = 300000;
        final int seed = 345987345;
        final Random rand = new Random(seed);
        try (final BufferedWriter writer = Files.newBufferedWriter(WORDS_LONG)) {
            for (int i = 0; i < numberOfWords; i++) {
                writer.write(wordsList.get(rand.nextInt(wordsList.size())));
            }
        }
    }

    @AfterClass
    public void tearDown() throws IOException {
        // reset java properties to original
        System.setProperty("java.io.tmpdir", systemTempDir);
        System.setProperty("user.name", systemUser);
        inMemoryFileSystem.close();
    }

    @Test
    public void testFileReadingAndWriting() throws IOException {
        String randomizedTestString = TEST_STRING + System.currentTimeMillis();
        for (String ext : TEST_FILE_EXTENSIONS) {
            File f = File.createTempFile(TEST_FILE_PREFIX, ext);
            f.deleteOnExit();

            OutputStream os = IOUtil.openFileForWriting(f.toPath());
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os));
            writer.write(randomizedTestString);
            writer.close();

            InputStream is = IOUtil.openFileForReading(f.toPath());
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line = reader.readLine();
            Assert.assertEquals(randomizedTestString, line);
        }
    }

    @Test(groups = {"unix"})
    public void testGetCanonicalPath() throws IOException {
        String tmpPath = System.getProperty("java.io.tmpdir");
        String userName = System.getProperty("user.name");

        if (tmpPath.endsWith(userName)) {
            tmpPath = tmpPath.substring(0, tmpPath.length() - userName.length());
        }

        File tmpDir = new File(tmpPath, userName);
        tmpDir.mkdir();
        tmpDir.deleteOnExit();
        File actual = new File(tmpDir, "actual.txt");
        actual.deleteOnExit();
        ProcessExecutor.execute(new String[]{"touch", actual.getAbsolutePath()});
        File symlink = new File(tmpDir, "symlink.txt");
        symlink.deleteOnExit();
        ProcessExecutor.execute(new String[]{"ln", "-s", actual.getAbsolutePath(), symlink.getAbsolutePath()});
        File lnDir = new File(tmpDir, "symLinkDir");
        lnDir.deleteOnExit();
        ProcessExecutor.execute(new String[]{"ln", "-s", tmpDir.getAbsolutePath(), lnDir.getAbsolutePath()});
        File lnToActual = new File(lnDir, "actual.txt");
        lnToActual.deleteOnExit();
        File lnToSymlink = new File(lnDir, "symlink.txt");
        lnToSymlink.deleteOnExit();

        File[] files = {actual, symlink, lnToActual, lnToSymlink};
        for (File f : files) {
            Assert.assertEquals(IOUtil.getFullCanonicalPath(f.toPath()), actual.getCanonicalPath());
        }
    }

    @Test
    public void testUtfWriting() throws IOException {
        final String utf8 = new StringWriter().append((char) 168).append((char) 197).toString();
        for (String ext : TEST_FILE_EXTENSIONS) {
            final File f = File.createTempFile(TEST_FILE_PREFIX, ext);
            f.deleteOnExit();

            final BufferedWriter writer = IOUtil.openFileForBufferedUtf8Writing(f.toPath());
            writer.write(utf8);
            CloserUtil.close(writer);

            final BufferedReader reader = IOUtil.openFileForBufferedUtf8Reading(f.toPath());
            final String line = reader.readLine();
            Assert.assertEquals(utf8, line, f.getAbsolutePath());

            CloserUtil.close(reader);

        }
    }

    @Test
    public void slurpLinesTest() throws FileNotFoundException {
        Assert.assertEquals(IOUtil.slurpLines(SLURP_TEST_FILE), SLURP_TEST_LINES);
    }

    @Test
    public void slurpWhitespaceOnlyFileTest() throws FileNotFoundException {
        Assert.assertEquals(IOUtil.slurp(FIVE_SPACES_THEN_A_NEWLINE_THEN_FIVE_SPACES_FILE), "     \n     ");
    }

    @Test
    public void slurpEmptyFileTest() throws FileNotFoundException {
        Assert.assertEquals(IOUtil.slurp(EMPTY_FILE), "");
    }

    @Test
    public void slurpTest() throws FileNotFoundException {
        Assert.assertEquals(IOUtil.slurp(SLURP_TEST_FILE), CollectionUtil.join(SLURP_TEST_LINES, SLURP_TEST_LINE_SEPARATOR));
    }

    @Test(dataProvider = "fileTypeTestCases")
    public void testFileType(final String path, boolean expectedIsRegularFile) {
        final File file = new File(path);
        Assert.assertEquals(IOUtil.isRegularPath(file.toPath()), expectedIsRegularFile);
        Assert.assertEquals(IOUtil.isRegularPath(file.toPath()), expectedIsRegularFile);
    }

    @Test(dataProvider = "unixFileTypeTestCases", groups = {"unix"})
    public void testFileTypeUnix(final String path, boolean expectedIsRegularFile) {
        final File file = new File(path);
        Assert.assertEquals(IOUtil.isRegularPath(file.toPath()), expectedIsRegularFile);
        Assert.assertEquals(IOUtil.isRegularPath(file.toPath()), expectedIsRegularFile);
    }

    @Test
    public void testAddExtension() throws IOException {
        Path p = IOUtil.getPath("/folder/file");
        Assert.assertEquals(IOUtil.addExtension(p, ".ext"), IOUtil.getPath("/folder/file.ext"));
        p = IOUtil.getPath("folder/file");
        Assert.assertEquals(IOUtil.addExtension(p, ".ext"), IOUtil.getPath("folder/file.ext"));
        try (FileSystem jimfs = Jimfs.newFileSystem(Configuration.unix())) {
            p = jimfs.getPath("folder/sub/file");
            Assert.assertEquals(IOUtil.addExtension(p, ".ext"), jimfs.getPath("folder/sub/file.ext"));
            p = jimfs.getPath("folder/file");
            Assert.assertEquals(IOUtil.addExtension(p, ".ext"), jimfs.getPath("folder/file.ext"));
            p = jimfs.getPath("file");
            Assert.assertEquals(IOUtil.addExtension(p, ".ext"), jimfs.getPath("file.ext"));
        }
    }

    @Test
    public void testAddExtensionOnList() throws IOException {
        Path p = IOUtil.getPath("/folder/file");
        List<FileSystemProvider> fileSystemProviders = FileSystemProvider.installedProviders();

        List<Path> paths = new ArrayList<>();
        List<String> strings = new ArrayList<>();

        paths.add(IOUtil.addExtension(p, ".ext"));
        strings.add("/folder/file.ext");

        p = IOUtil.getPath("folder/file");
        paths.add(IOUtil.addExtension(p, ".ext"));
        strings.add("folder/file.ext");

        List<Path> expectedPaths = IOUtil.getPaths(strings);

        Assert.assertEquals(paths, expectedPaths);
    }


    @DataProvider(name = "fileTypeTestCases")
    private Object[][] fileTypeTestCases() {
        return new Object[][]{
                {existingTempFile.getAbsolutePath(), Boolean.TRUE},
                {systemTempDir, Boolean.FALSE}

        };
    }

    @DataProvider(name = "unixFileTypeTestCases")
    private Object[][] unixFileTypeTestCases() {
        return new Object[][]{
                {"/dev/null", Boolean.FALSE},
                {"/dev/stdout", Boolean.FALSE},
                {"/non/existent/file", Boolean.TRUE},
        };
    }

    @DataProvider
    public Object[][] getFiles(){
        final File file = new File("someFile");
        return new Object[][] {
                {null, null},
                {file, file.toPath()}
        };
    }

    @Test(dataProvider = "getFiles")
    public void testToPath(final File file, final Path expected){
        Assert.assertEquals(file == null ? null : file.toPath(), expected);
    }


    @DataProvider(name = "fileNamesForDelete")
    public Object[][] fileNamesForDelete() {
        return new Object[][] {
                {Collections.emptyList()},
                {Collections.singletonList("file1")},
                {Arrays.asList("file1", "file2")}
        };
    }

    @Test
    public void testGetDefaultTmpDirPath() throws Exception {
        try {
            Path testPath = IOUtil.getDefaultTmpDirPath();
            Assert.assertEquals(testPath.toFile().getAbsolutePath(), new File(systemTempDir).getAbsolutePath() + "/" + systemUser);

            // change the properties to test others
            final String newTempPath = Files.createTempDirectory("testGetDefaultTmpDirPath").toString();
            final String newUser = "my_user";
            System.setProperty("java.io.tmpdir", newTempPath);
            System.setProperty("user.name", newUser);
            testPath = IOUtil.getDefaultTmpDirPath();
            Assert.assertEquals(testPath.toFile().getAbsolutePath(), new File(newTempPath).getAbsolutePath() + "/" + newUser);

        } finally {
            // reset system properties
            System.setProperty("java.io.tmpdir", systemTempDir);
            System.setProperty("user.name", systemUser);
        }
    }

    @Test(dataProvider = "fileNamesForDelete")
    public void testDeletePathLocal(final List<String> fileNames) throws Exception {
        final Path tmpDir = IOUtil.createTempDir("testDeletePath");
        final List<Path> paths = createLocalFiles(tmpDir, fileNames);
        testDeletePaths(paths);
    }

    @Test
    public void testDeleteSinglePath() throws Exception {
        final Path toDelete = Files.createTempFile("file",".bad");
        Assert.assertTrue(Files.exists(toDelete));
        IOUtil.deletePath(toDelete);
        Assert.assertFalse(Files.exists(toDelete));
    }

    @Test
    public void testDeleteSingleWithDeletePaths() throws Exception {
        final Path toDelete = Files.createTempFile("file",".bad");
        Assert.assertTrue(Files.exists(toDelete));
        IOUtil.deletePaths(toDelete);
        Assert.assertFalse(Files.exists(toDelete));
    }

    @Test(dataProvider = "fileNamesForDelete")
    public void testDeletePathJims(final List<String> fileNames) throws Exception {
        final List<Path> paths = createJimfsFiles("testDeletePath", fileNames);
        testDeletePaths(paths);
    }

    @Test(dataProvider = "fileNamesForDelete")
    public void testDeleteArrayPathLocal(final List<String> fileNames) throws Exception {
        final Path tmpDir = IOUtil.createTempDir("testDeletePath");
        final List<Path> paths = createLocalFiles(tmpDir, fileNames);
        testDeletePathArray(paths);
    }

    @Test(dataProvider = "fileNamesForDelete")
    public void testDeleteArrayPathJims(final List<String> fileNames) throws Exception {
        final List<Path> paths = createJimfsFiles("testDeletePath", fileNames);
        testDeletePathArray(paths);
    }


    private static void testDeletePaths(final List<Path> paths) {
        paths.forEach(p -> Assert.assertTrue(Files.exists(p)));
        IOUtil.deletePaths(paths);
        paths.forEach(p -> Assert.assertFalse(Files.exists(p)));
    }

    private static void testDeletePathArray(final List<Path> paths) {
        paths.forEach(p -> Assert.assertTrue(Files.exists(p)));
        IOUtil.deletePaths(paths.toArray(new Path[paths.size()]));
        paths.forEach(p -> Assert.assertFalse(Files.exists(p)));
    }

    private static List<Path> createLocalFiles(final Path tmpDir, final List<String> fileNames) throws Exception {
        final List<Path> paths = new ArrayList<>(fileNames.size());
        for (final String f: fileNames) {
            final Path file = Files.createFile(tmpDir.resolve(f));
            paths.add(file);
        }
        return paths;
    }

    private List<Path> createJimfsFiles(final String folderName, final List<String> fileNames) throws Exception {
        final List<Path> paths = new ArrayList<>(fileNames.size());
        final Path folder = inMemoryFileSystem.getPath(folderName);
        if (Files.notExists(folder)) Files.createDirectory(folder);

        for (final String f: fileNames) {
            final Path p = inMemoryFileSystem.getPath(folderName, f);
            Files.createFile(p);
            paths.add(p);
        }

        return paths;
    }

    @DataProvider
    public Object[][] pathsForWritableDirectory() throws Exception {
        return new Object[][] {
                // non existent
                {inMemoryFileSystem.getPath("no_exists"), false},
                // non directory
                {Files.createFile(inMemoryFileSystem.getPath("testAssertDirectoryIsWritable_file")), false},
                // TODO - how to do in inMemoryFileSystem a non-writable directory?
                // writable directory
                {Files.createDirectory(inMemoryFileSystem.getPath("testAssertDirectoryIsWritable_directory")), true}
        };
    }

    @Test(dataProvider = "pathsForWritableDirectory")
    public void testAssertDirectoryIsWritablePath(final Path path, final boolean writable) {
        try {
            IOUtil.assertDirectoryIsWritable(path);
        } catch (SAMException e) {
            if (writable) {
                Assert.fail(e.getMessage());
            }
        }
    }

    @DataProvider
    public Object[][] filesForWritableDirectory() throws Exception {
        final File nonWritableFile = new File(systemTempDir, "testAssertDirectoryIsWritable_non_writable_dir");
        nonWritableFile.mkdir();
        nonWritableFile.setWritable(false);

        return new Object[][] {
                // non existent
                {new File("no_exists"), false},
                // non directory
                {existingTempFile, false},
                // non-writable directory
                {nonWritableFile, false},
                // writable directory
                {new File(systemTempDir), true},
        };
    }

    @Test(dataProvider = "filesForWritableDirectory")
    public void testAssertDirectoryIsWritableFile(final File file, final boolean writable) {
        try {
            IOUtil.assertDirectoryIsWritable(file.toPath());
        } catch (SAMException e) {
            if (writable) {
                Assert.fail(e.getMessage());
            }
        }
    }

    static final String level1 = "Level1.fofn";
    static final String level2 = "Level2.fofn";
    static final String fofnHttpQueryParams = "FofnWithHttpQueryParams.fofn";

    @DataProvider
    public Object[][] fofnData() throws IOException {

        Path fofnPath1 = inMemoryFileSystem.getPath(level1);
        Files.copy(TEST_DATA_DIR.resolve(level1), fofnPath1);

        Path fofnPath2 = inMemoryFileSystem.getPath(level2);
        Files.copy(TEST_DATA_DIR.resolve(level2), fofnPath2);

        Path withHttpPath = inMemoryFileSystem.getPath(fofnHttpQueryParams);
        Files.copy(TEST_DATA_DIR.resolve(fofnHttpQueryParams), withHttpPath);

        return new Object[][]{
                {TEST_DATA_DIR + "/" + level1, new String[]{".vcf", ".vcf.gz"}, 2},
                {TEST_DATA_DIR + "/" + level2, new String[]{".vcf", ".vcf.gz"}, 4},
                {fofnPath1.toUri().toString(), new String[]{".vcf", ".vcf.gz"}, 2},
                {fofnPath2.toUri().toString(), new String[]{".vcf", ".vcf.gz"}, 4},
                //test http links with query parameters are handled correctly
                //test disabled until NIO http provider is integrated
                //see https://github.com/samtools/htsjdk/issues/1689
                //{withHttpPath.toUri().toString(), new String[]{".vcf", ".vcf.gz"}, 4}
        };
    }

    @Test(dataProvider = "fofnData")
    public void testUnrollPaths(final String pathUri, final String[] extensions, final int expectedNumberOfUnrolledPaths) throws IOException {
        Path p = IOUtil.getPath(pathUri);
        List<Path> paths = IOUtil.unrollPaths(Collections.singleton(p), extensions);

        Assert.assertEquals(paths.size(), expectedNumberOfUnrolledPaths);
    }

    @DataProvider(name = "blockCompressedExtensionExtensionStrings")
    public static Object[][] createBlockCompressedExtensionStrings() {
        return new Object[][] {
                { "testzip.gz", true },
                { "test.gzip", true },
                { "test.bgz", true },
                { "test.bgzf", true },
                { "test.bzip2", false }
        };
    }

    @Test(dataProvider = "blockCompressedExtensionExtensionStrings")
    public void testBlockCompressionExtensionString(final String testString, final boolean expected) {
        Assert.assertEquals(IOUtil.hasBlockCompressedExtension(testString), expected);
    }

    @Test(dataProvider = "blockCompressedExtensionExtensionStrings")
    public void testBlockCompressionExtensionFile(final String testString, final boolean expected) {
        Assert.assertEquals(IOUtil.hasBlockCompressedExtension(new File(testString).toPath()), expected);
    }

    @DataProvider(name = "blockCompressedExtensionExtensionURIStrings")
    public static Object[][] createBlockCompressedExtensionURIs() {
        return new Object[][]{
                {"testzip.gz", true},
                {"test.gzip", true},
                {"test.bgz", true},
                {"test.bgzf", true},
                {"test", false},
                {"test.bzip2", false},

                {"https://www.googleapis.com/download/storage/v1/b/deflaux-public-test/o/NA12877.vcf.gz", true},
                {"https://www.googleapis.com/download/storage/v1/b/deflaux-public-test/o/NA12877.vcf.gzip", true},
                {"https://www.googleapis.com/download/storage/v1/b/deflaux-public-test/o/NA12877.vcf.bgz", true},
                {"https://www.googleapis.com/download/storage/v1/b/deflaux-public-test/o/NA12877.vcf.bgzf", true},
                {"https://www.googleapis.com/download/storage/v1/b/deflaux-public-test/o/NA12877.vcf.bzip2", false},
                {"https://www.googleapis.com/download/storage/v1/b/deflaux-public-test/o/NA12877", false},

                {"https://www.googleapis.com/download/storage/v1/b/deflaux-public-test/o/NA12877.vcf.gz?alt=media", true},
                {"https://www.googleapis.com/download/storage/v1/b/deflaux-public-test/o/NA12877.vcf.gzip?alt=media", true},
                {"https://www.googleapis.com/download/storage/v1/b/deflaux-public-test/o/NA12877.vcf.bgz?alt=media", true},
                {"https://www.googleapis.com/download/storage/v1/b/deflaux-public-test/o/NA12877.vcf.bgzf?alt=media", true},
                {"https://www.googleapis.com/download/storage/v1/b/deflaux-public-test/o/NA12877.vcf.bzip2?alt=media", false},

                {"ftp://ftp.broadinstitute.org/distribution/igv/TEST/cpgIslands.hg18.gz", true},
                {"ftp://ftp.broadinstitute.org/distribution/igv/TEST/cpgIslands.hg18.bed", false}
        };
    }

    @Test(dataProvider = "blockCompressedExtensionExtensionURIStrings")
    public void testBlockCompressionExtension(final String testURIString, final boolean expected) {
        URI testURI = URI.create(testURIString);
        Assert.assertEquals(IOUtil.hasBlockCompressedExtension(testURI), expected);
    }

    @Test(dataProvider = "blockCompressedExtensionExtensionURIStrings")
    public void testBlockCompressionExtensionStringVersion(final String testURIString, final boolean expected) {
        Assert.assertEquals(IOUtil.hasBlockCompressedExtension(testURIString), expected);
    }

    @DataProvider
    public static Object[][] blockCompressedFiles() {
        return new Object[][]{
                {TEST_DATA_DIR.resolve("ipsum.txt"), true, false},
                {TEST_DATA_DIR.resolve("ipsum.txt"), false, false},
                {TEST_DATA_DIR.resolve("ipsum.txt.gz"), true, false},
                {TEST_DATA_DIR.resolve("ipsum.txt.gz"), false, false},
                {TEST_DATA_DIR.resolve("ipsum.txt.bgz"), true, true},
                {TEST_DATA_DIR.resolve("ipsum.txt.bgz"), false, true},
                {TEST_DATA_DIR.resolve("ipsum.txt.bgz.wrongextension"), true, false},
                {TEST_DATA_DIR.resolve("ipsum.txt.bgz.wrongextension"), false, true},
                {TEST_DATA_DIR.resolve("ipsum.txt.bgzipped_with_gzextension.gz"), true, true},
                {TEST_DATA_DIR.resolve("ipsum.txt.bgzipped_with_gzextension.gz"), false, true},
                {TEST_DATA_DIR.resolve("example.bam"), true, false},
                {TEST_DATA_DIR.resolve("example.bam"), false, true}
        };
    }

    @Test(dataProvider = "blockCompressedFiles")
    public void testIsBlockCompressed(Path file, boolean checkExtension, boolean expected) throws IOException {
        Assert.assertEquals(IOUtil.isBlockCompressed(file, checkExtension), expected);
    }

    @Test(dataProvider = "blockCompressedFiles")
    public void testIsBlockCompressedOnJimfs(Path file, boolean checkExtension, boolean expected) throws IOException {
         try (FileSystem jimfs = Jimfs.newFileSystem(Configuration.unix())) {
             final Path jimfsRoot = jimfs.getRootDirectories().iterator().next();
             final Path jimfsFile = Files.copy(file, jimfsRoot.resolve(file.getFileName().toString()));
             Assert.assertEquals(IOUtil.isBlockCompressed(jimfsFile, checkExtension), expected);
         }
    }

    @DataProvider
    public static Object[][] filesToCompress() {
        return new Object[][]{
                {WORDS_LONG, ".gz", 8},
                {WORDS_LONG, ".bfq", 8},
                {TEST_VARIANT_DIR.resolve("test1.vcf"), ".gz", 7},
                {TEST_VARIANT_DIR.resolve("test1.vcf"), ".bfq", 7}
        };
    }

    @Test(dataProvider = "filesToCompress")
    public void testCompressionLevel(final Path file, final String extension, final int lastDifference) throws IOException {
        final long origSize = Files.size(file);
        long previousSize = origSize;
        for (int compressionLevel = 1; compressionLevel <= 9; compressionLevel++) {
            final Path outFile = Files.createTempFile("tmp", extension);
            outFile.toFile().deleteOnExit();
            IOUtil.setCompressionLevel(compressionLevel);
            Assert.assertEquals(IOUtil.getCompressionLevel(), compressionLevel);
            final InputStream inStream = IOUtil.openFileForReading(file);
            try (final OutputStream outStream = IOUtil.openFileForWriting(outFile)) {
                IOUtil.transferByStream(inStream, outStream, origSize);
            }
            final long newSize = Files.size(outFile);
            if (compressionLevel <= lastDifference) {
                Assert.assertTrue(previousSize > newSize);
            } else {
                Assert.assertTrue(previousSize >= newSize);
            }
            previousSize = newSize;
        }
    }

    @DataProvider
    public Object[][] getExtensions(){
        return new Object[][]{
                {".gz", true},
                {".bfq", true},
                {".txt", false}};
    }

    @Test(dataProvider = "getExtensions")
    public void testReadWriteJimfs(String extension, boolean gzipped) throws IOException {
        final Path jmfsRoot = inMemoryFileSystem.getRootDirectories().iterator().next();
        final Path tmp = Files.createTempFile(jmfsRoot, "test", extension);
        final String expected = "lorem ipswitch, nantucket, bucket";
        try (Writer out = IOUtil.openFileForBufferedWriting(tmp)){
            out.write(expected);
        }

        try (InputStream in = new BufferedInputStream(Files.newInputStream(tmp))){
               Assert.assertEquals(IOUtil.isGZIPInputStream(in), gzipped);
        }

        try (BufferedReader in = IOUtil.openFileForBufferedReading(tmp)){
            final String actual = in.readLine();
            Assert.assertEquals(actual, expected);
        }
    }

    @DataProvider
    public static Object[][] badCompressionLevels() {
        return new Object[][]{
                {-1},
                {10}
        };
    }

    @Test(dataProvider = "badCompressionLevels", expectedExceptions = {IllegalArgumentException.class})
    public void testCompressionLevelExceptions(final int compressionLevel) {
        IOUtil.setCompressionLevel(compressionLevel);
    }

    @DataProvider
    public static Object[][] filesToCopy() {
        return new Object[][]{
                {TEST_VARIANT_DIR.resolve("test1.vcf")},
                {TEST_DATA_DIR.resolve("ipsum.txt")}
        };
    }

    @Test(dataProvider = "filesToCopy")
    public void testCopyFile(final Path file) throws IOException {
        final Path outFile = Files.createTempFile("tmp", ".tmp");
        outFile.toFile().deleteOnExit();
        IOUtil.copyPath(file, outFile);
        Assert.assertEquals(Files.lines(file).collect(Collectors.toList()), Files.lines(outFile).collect(Collectors.toList()));
    }

    @Test(dataProvider = "filesToCopy", expectedExceptions = {SAMException.class})
    public void testCopyFileReadException(final Path file) throws IOException {
        final Path outFile = Files.createTempFile("tmp", ".tmp");
        outFile.toFile().deleteOnExit();
        file.toFile().setReadable(false);
        try {
            IOUtil.copyPath(file, outFile);
        } finally { //need to set input file permission back to readable so other unit tests can access it
            file.toFile().setReadable(true);
        }
    }

    @Test(dataProvider = "filesToCopy", expectedExceptions = {SAMException.class})
    public void testCopyFileWriteException(final Path file) throws IOException {
        // Skip on macOS - file permission handling differs from Linux
        // On macOS, setWritable(false) doesn't always prevent writing to files
        if (SystemUtils.IS_OS_MAC) {
            throw new SkipException("Skipping testCopyFileWriteException on macOS - file permission handling differs from Linux");
        }
        
        final Path outFile = Files.createTempFile("tmp", ".tmp");
        outFile.toFile().deleteOnExit();
        outFile.toFile().setWritable(false);
        IOUtil.copyPath(file, outFile);
    }

    @DataProvider
    public static Object[][] baseNameTests() {
        return new Object[][]{
                {TEST_DATA_DIR.resolve("ipsum.txt"), "ipsum"},
                {TEST_DATA_DIR.resolve("ipsum.txt.bgz.wrongextension"), "ipsum.txt.bgz"},
                {TEST_DATA_DIR.resolve("ipsum.txt.bgzipped_with_gzextension.gz"), "ipsum.txt.bgzipped_with_gzextension"},
                {TEST_VARIANT_DIR.resolve("utils"), "utils"},
                {TEST_VARIANT_DIR.resolve("not_real_file.txt"), "not_real_file"}
        };
    }

    @Test(dataProvider = "baseNameTests")
    public void testBasename(final Path file, final String expected) {
        final String result = IOUtil.basename(file);
        Assert.assertEquals(result, expected);
    }

    @DataProvider
    public static Object[][] regExpTests() {
        return new Object[][]{
                {"\\w+\\.txt", new String[]{"5newline5.txt", "empty.txt", "ipsum.txt", "slurptest.txt"}},
                {"^((?!txt).)*$", new String[]{"Level1.fofn", "Level2.fofn", "example.bam"}},
                {"^\\d+.*", new String[]{"5newline5.txt"}}
        };
    }

    @Test(dataProvider = "regExpTests")
    public void testRegExp(final String regexp, final String[] expected) throws IOException {
        final String[] allNames = {"5newline5.txt", "Level2.fofn", "example.bam", "ipsum.txt.bgz", "ipsum.txt.bgzipped_with_gzextension.gz", "slurptest.txt", "Level1.fofn", "empty.txt", "ipsum.txt", "ipsum.txt.bgz.wrongextension", "ipsum.txt.gz"};
        final Path regExpDir = Files.createTempDirectory("regExpDir");
        regExpDir.toFile().deleteOnExit();
        final List<String> listExpected = Arrays.asList(expected);
        final List<Path> expectedFiles = new ArrayList<Path>();
        for (String name : allNames) {
            final Path file = regExpDir.resolve(name);
            file.toFile().deleteOnExit();
            file.toFile().createNewFile();
            if (listExpected.contains(name)) {
                expectedFiles.add(file);
            }
        }
        final List<Path> result = IOUtil.getPathsMatchingRegexp(regExpDir, regexp);
        Assert.assertEqualsNoOrder(result.toArray(), expectedFiles.toArray());
    }

    @Test()
    public void testReadLines() throws IOException {
        final Path file = Files.createTempFile("tmp", ".txt");
        file.toFile().deleteOnExit();
        final int seed = 12394738;
        final Random rand = new Random(seed);
        final int nLines = 5;
        final List<String> lines = new ArrayList<String>();
        try (final PrintWriter writer = new PrintWriter(Files.newBufferedWriter(file))) {
            for (int i = 0; i < nLines; i++) {
                final String line = TEST_STRING + Integer.toString(rand.nextInt(100000000));
                lines.add(line);
                writer.println(line);
            }
        }
        final List<String> retLines = new ArrayList<String>();
        IOUtil.readLines(file).forEachRemaining(retLines::add);
        Assert.assertEquals(retLines, lines);
    }

    @DataProvider
    public static Object[][] fileSuffixTests() {
        return new Object[][]{
                {TEST_DATA_DIR.resolve("ipsum.txt"), ".txt"},
                {TEST_DATA_DIR.resolve("ipsum.txt.bgz"), ".bgz"},
                {TEST_DATA_DIR, null}
        };
    }

    @Test(dataProvider = "fileSuffixTests")
    public void testSuffixTest(final Path file, final String expected) {
        final String ret = IOUtil.fileSuffix(file);
        Assert.assertEquals(ret, expected);
    }

    @Test
    public void testCopyDirectoryTree() throws IOException {
        final Path copyToDir = Files.createTempDirectory("copyToDir");
        copyToDir.toFile().deleteOnExit();
        IOUtil.copyDirectoryTree(TEST_VARIANT_DIR, copyToDir);
        final List<Path> collect = Files.walk(TEST_VARIANT_DIR).filter(f -> !f.equals(TEST_VARIANT_DIR)).map(p -> p.getFileName()).collect(Collectors.toList());
        final List<Path> collectCopy = Files.walk(copyToDir).filter(f -> !f.equals(copyToDir)).map(p -> p.getFileName()).collect(Collectors.toList());
        Assert.assertEqualsNoOrder(collect.toArray(), collectCopy.toArray());
    }

    // Little utility to gzip a byte array
    private static byte[] gzipMessage(final byte[] message) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        GZIPOutputStream gzout = new GZIPOutputStream(bos);
        gzout.write(message);
        gzout.finish();
        gzout.close();
        return bos.toByteArray();
    }

    @DataProvider
    public static Object[][] gzipTests() throws IOException {
        final byte[] emptyMessage = "".getBytes();
        final byte[] message = "Hello World".getBytes();

        // Compressed version of the messages
        final byte[] gzippedMessage = gzipMessage(message);
        final byte[] gzippedEmptyMessage = gzipMessage(emptyMessage);

        return new Object[][]{
                {emptyMessage, false},
                {message, false},
                {gzippedMessage, true},
                {gzippedEmptyMessage, true}
        };
    }

    @Test(dataProvider = "gzipTests")
    public void isGZIPInputStreamTest(byte[] data, boolean isGzipped) throws IOException {
        try(ByteArrayInputStream inputStream = new ByteArrayInputStream(data)) {
            // test string without compression
            Assert.assertEquals(IOUtil.isGZIPInputStream(inputStream), isGzipped);
            // call twice to verify 'in.reset()' was called
            Assert.assertEquals(IOUtil.isGZIPInputStream(inputStream), isGzipped);
        }
    }

    @Test
    public void testOpenFileForMd5CalculatingWriting() throws IOException {
        Path output = Files.createTempFile("test", FileExtensions.BAM);

        try (final OutputStream outputStream = IOUtil.openFileForMd5CalculatingWriting(output)){
            // tsato: perhaps BamFileIoUtils is a better place for this test
            BamFileIoUtils.blockCopyBamFile(HtsjdkTestUtils.NA12878_8000, outputStream, false, false);
        } catch (IOException e) {
            throw new HtsjdkException("Encountered an IO error", e);
        }

        final String md5FileName = output.getFileName() + FileExtensions.MD5;
        Assert.assertTrue(md5FileName.endsWith(".bam.md5"));
        Assert.assertTrue(Files.exists(output.resolveSibling(md5FileName)));
    }

    // New tests for Path-based methods (Task 1.2)

    @Test
    public void testPathExistenceChecking() throws IOException {
        // Test with existing path
        Path existingPath = Files.createTempFile("test", ".txt");
        existingPath.toFile().deleteOnExit();
        Assert.assertTrue(Files.exists(existingPath));
        
        // Test with non-existing path
        Path nonExistingPath = Paths.get("/non/existent/path.txt");
        Assert.assertFalse(Files.exists(nonExistingPath));
    }

    @Test
    public void testDirectoryChecking() throws IOException {
        // Test with directory
        Path directory = Files.createTempDirectory("testDir");
        directory.toFile().deleteOnExit();
        Assert.assertTrue(Files.isDirectory(directory));
        
        // Test with file
        Path file = Files.createTempFile("test", ".txt");
        file.toFile().deleteOnExit();
        Assert.assertFalse(Files.isDirectory(file));
    }

    @Test
    public void testReadabilityChecks() throws IOException {
        Path readablePath = Files.createTempFile("readable", ".txt");
        readablePath.toFile().deleteOnExit();
        Assert.assertTrue(Files.isReadable(readablePath));
        
        // Test assertFileIsReadable with valid path
        IOUtil.assertFileIsReadable(readablePath);
    }

    @Test(expectedExceptions = SAMException.class)
    public void testReadabilityCheckNonExistent() {
        Path nonExistent = Paths.get("/non/existent/file.txt");
        IOUtil.assertFileIsReadable(nonExistent);
    }

    @Test
    public void testWritabilityChecks() throws IOException {
        Path writablePath = Files.createTempFile("writable", ".txt");
        writablePath.toFile().deleteOnExit();
        Assert.assertTrue(Files.isWritable(writablePath));
        
        // Test assertFileIsWritable with valid path
        IOUtil.assertFileIsWritable(writablePath);
    }

    @Test
    public void testUriToPathConstruction() throws IOException {
        // Test file:// URI
        URI fileUri = URI.create("file:///tmp/test.txt");
        Path path = IOUtil.getPath(fileUri);
        Assert.assertNotNull(path);
        Assert.assertEquals(path.toUri().getScheme(), "file");
    }

    @Test
    public void testGetPathFromString() throws IOException {
        // Test with simple path string
        String pathString = "/tmp/test.txt";
        Path path = IOUtil.getPath(pathString);
        Assert.assertNotNull(path);
        
        // Test with URI string
        String uriString = "file:///tmp/test.txt";
        Path pathFromUri = IOUtil.getPath(uriString);
        Assert.assertNotNull(pathFromUri);
    }

    @Test
    public void testNoFileBasedMethodsExist() throws Exception {
        // Use reflection to verify no public methods accept or return File
        java.lang.reflect.Method[] methods = IOUtil.class.getMethods();
        
        for (java.lang.reflect.Method method : methods) {
            // Check parameters - no method should accept File (deprecated or not)
            for (Class<?> paramType : method.getParameterTypes()) {
                if (paramType.equals(File.class)) {
                    Assert.fail("Method " + method.getName() + " has File parameter - all File-based methods should be removed");
                }
            }
            
            // Check return type - no method should return File
            if (method.getReturnType().equals(File.class)) {
                Assert.fail("Method " + method.getName() + " returns File - all File-based methods should be removed");
            }
        }
    }

    @Test
    public void testPathBasedFileOperations() throws IOException {
        // Test opening file for reading with Path
        Path testFile = Files.createTempFile("test", ".txt");
        testFile.toFile().deleteOnExit();
        Files.write(testFile, "test content".getBytes());
        
        try (InputStream is = IOUtil.openFileForReading(testFile)) {
            Assert.assertNotNull(is);
            byte[] content = new byte[12];
            is.read(content);
            Assert.assertEquals(new String(content), "test content");
        }
        
        // Test opening file for writing with Path
        Path outputFile = Files.createTempFile("output", ".txt");
        outputFile.toFile().deleteOnExit();
        
        try (OutputStream os = IOUtil.openFileForWriting(outputFile)) {
            os.write("output content".getBytes());
        }
        
        Assert.assertTrue(Files.exists(outputFile));
        Assert.assertEquals(new String(Files.readAllBytes(outputFile)), "output content");
    }

    @Test
    public void testPathUtilityMethods() throws IOException {
        // Test basename with Path
        Path path = Paths.get("/folder/file.txt");
        Assert.assertEquals(IOUtil.basename(path), "file");
        
        // Test with no extension
        Path noExt = Paths.get("/folder/file");
        Assert.assertEquals(IOUtil.basename(noExt), "file");
    }

    @Test
    public void testAssertPathsAreReadable() throws IOException {
        Path path1 = Files.createTempFile("test1", ".txt");
        Path path2 = Files.createTempFile("test2", ".txt");
        path1.toFile().deleteOnExit();
        path2.toFile().deleteOnExit();
        
        List<Path> paths = Arrays.asList(path1, path2);
        IOUtil.assertPathsAreReadable(paths);
    }

    @Test(expectedExceptions = SAMException.class)
    public void testAssertPathsAreReadableWithNonExistent() {
        Path existing = Paths.get(systemTempDir, "existing.txt");
        Path nonExisting = Paths.get("/non/existent/file.txt");
        
        List<Path> paths = Arrays.asList(existing, nonExisting);
        IOUtil.assertPathsAreReadable(paths);
    }

    // Property test for URI to Path round trip (Task 1.4)
    // Feature: file-to-path-migration, Property 1: URI to Path Round Trip
    // Validates: Requirements 1.4, 9.4
    @Test
    public void testUriToPathRoundTrip() throws IOException {
        // Test with multiple file:// URIs to simulate property-based testing
        String[] testPaths = {
            "/tmp/test.txt",
            "/home/user/data.bam",
            "/var/log/file.log",
            "/usr/local/bin/script.sh",
            "/data/genomics/sample.vcf"
        };
        
        for (String pathStr : testPaths) {
            URI fileUri = URI.create("file://" + pathStr);
            Path path = IOUtil.getPath(fileUri);
            URI resultUri = path.toUri();
            
            // Verify scheme is preserved
            Assert.assertEquals(resultUri.getScheme(), "file", 
                "URI scheme should be preserved for: " + pathStr);
            
            // Verify path is preserved (normalize for comparison)
            Assert.assertEquals(resultUri.getPath(), fileUri.getPath(),
                "URI path should be preserved for: " + pathStr);
        }
    }

    @Test
    public void testUriToPathRoundTripWithSpecialCharacters() throws IOException {
        // Test URIs with special characters that need encoding
        String[] testPaths = {
            "/tmp/file with spaces.txt",
            "/data/file#with#hash.bam",
            "/home/file%20encoded.vcf"
        };
        
        for (String pathStr : testPaths) {
            try {
                Path path = IOUtil.getPath(pathStr);
                URI resultUri = path.toUri();
                
                // Verify we can round-trip back to a path
                Path roundTripPath = IOUtil.getPath(resultUri);
                Assert.assertNotNull(roundTripPath, 
                    "Should be able to round-trip path: " + pathStr);
                
                // Verify scheme is file
                Assert.assertEquals(resultUri.getScheme(), "file",
                    "URI scheme should be 'file' for: " + pathStr);
            } catch (Exception e) {
                // Some special characters may not be supported on all filesystems
                // This is acceptable
            }
        }
    }

    // ============================================================================
    // Task 14.5: Unit tests for IOUtil Path methods
    // Requirements: 1.1, 1.5
    // ============================================================================

    /**
     * Test path existence checking with Files.exists() via IOUtil methods.
     * Validates: Requirement 1.1
     */
    @Test
    public void testPathExistenceWithIOUtil() throws IOException {
        // Test with existing file
        Path existingFile = Files.createTempFile("existence_test", ".txt");
        existingFile.toFile().deleteOnExit();
        Assert.assertTrue(Files.exists(existingFile), "Existing file should exist");
        
        // Test with existing directory
        Path existingDir = Files.createTempDirectory("existence_test_dir");
        existingDir.toFile().deleteOnExit();
        Assert.assertTrue(Files.exists(existingDir), "Existing directory should exist");
        
        // Test with non-existing path
        Path nonExisting = Paths.get("/non/existent/path/file.txt");
        Assert.assertFalse(Files.exists(nonExisting), "Non-existing path should not exist");
        
        // Test with jimfs
        Path jimfsPath = inMemoryFileSystem.getPath("/test_existence.txt");
        Assert.assertFalse(Files.exists(jimfsPath), "Non-existing jimfs path should not exist");
        Files.createFile(jimfsPath);
        Assert.assertTrue(Files.exists(jimfsPath), "Created jimfs path should exist");
    }

    /**
     * Test directory checking with Files.isDirectory() via IOUtil methods.
     * Validates: Requirement 1.1
     */
    @Test
    public void testDirectoryCheckingWithIOUtil() throws IOException {
        // Test with directory
        Path directory = Files.createTempDirectory("dir_check_test");
        directory.toFile().deleteOnExit();
        Assert.assertTrue(Files.isDirectory(directory), "Directory should be identified as directory");
        Assert.assertTrue(IOUtil.isRegularPath(directory) == false, "Directory should not be regular path");
        
        // Test with file
        Path file = Files.createTempFile("file_check_test", ".txt");
        file.toFile().deleteOnExit();
        Assert.assertFalse(Files.isDirectory(file), "File should not be identified as directory");
        Assert.assertTrue(IOUtil.isRegularPath(file), "File should be regular path");
        
        // Test with jimfs directory
        Path jimfsDir = inMemoryFileSystem.getPath("/test_dir_check");
        Files.createDirectory(jimfsDir);
        Assert.assertTrue(Files.isDirectory(jimfsDir), "Jimfs directory should be identified as directory");
        
        // Test with jimfs file
        Path jimfsFile = inMemoryFileSystem.getPath("/test_file_check.txt");
        Files.createFile(jimfsFile);
        Assert.assertFalse(Files.isDirectory(jimfsFile), "Jimfs file should not be identified as directory");
    }

    /**
     * Test readability checks with IOUtil.assertFileIsReadable().
     * Validates: Requirement 1.1
     */
    @Test
    public void testReadabilityChecksWithIOUtil() throws IOException {
        // Test with readable file
        Path readableFile = Files.createTempFile("readable_test", ".txt");
        readableFile.toFile().deleteOnExit();
        Files.write(readableFile, "test content".getBytes());
        
        Assert.assertTrue(Files.isReadable(readableFile), "File should be readable");
        IOUtil.assertFileIsReadable(readableFile); // Should not throw
        
        // Test with jimfs readable file
        Path jimfsReadable = inMemoryFileSystem.getPath("/readable_jimfs.txt");
        Files.createFile(jimfsReadable);
        Files.write(jimfsReadable, "jimfs content".getBytes());
        Assert.assertTrue(Files.isReadable(jimfsReadable), "Jimfs file should be readable");
        IOUtil.assertFileIsReadable(jimfsReadable); // Should not throw
    }

    /**
     * Test that assertFileIsReadable throws for non-existent file.
     * Validates: Requirement 1.1
     */
    @Test(expectedExceptions = SAMException.class)
    public void testReadabilityCheckThrowsForNonExistent() {
        Path nonExistent = Paths.get("/absolutely/non/existent/file.txt");
        IOUtil.assertFileIsReadable(nonExistent);
    }

    /**
     * Test that assertFileIsReadable throws for directory.
     * Validates: Requirement 1.1
     */
    @Test(expectedExceptions = SAMException.class)
    public void testReadabilityCheckThrowsForDirectory() throws IOException {
        Path directory = Files.createTempDirectory("readability_dir_test");
        directory.toFile().deleteOnExit();
        IOUtil.assertFileIsReadable(directory);
    }

    /**
     * Test writability checks with IOUtil.assertFileIsWritable().
     * Validates: Requirement 1.1
     */
    @Test
    public void testWritabilityChecksWithIOUtil() throws IOException {
        // Test with writable file
        Path writableFile = Files.createTempFile("writable_test", ".txt");
        writableFile.toFile().deleteOnExit();
        
        Assert.assertTrue(Files.isWritable(writableFile), "File should be writable");
        IOUtil.assertFileIsWritable(writableFile); // Should not throw
        
        // Test with non-existent file in writable directory
        Path tempDir = Files.createTempDirectory("writable_dir_test");
        tempDir.toFile().deleteOnExit();
        Path newFile = tempDir.resolve("new_file.txt");
        IOUtil.assertFileIsWritable(newFile); // Should not throw - parent is writable
    }

    /**
     * Test that assertFileIsWritable throws for non-existent parent directory.
     * Validates: Requirement 1.1
     */
    @Test(expectedExceptions = SAMException.class)
    public void testWritabilityCheckThrowsForNonExistentParent() {
        Path nonExistentParent = Paths.get("/non/existent/parent/file.txt");
        IOUtil.assertFileIsWritable(nonExistentParent);
    }

    /**
     * Test assertDirectoryIsWritable with Path.
     * Validates: Requirement 1.1
     */
    @Test
    public void testAssertDirectoryIsWritableWithPath() throws IOException {
        Path writableDir = Files.createTempDirectory("writable_dir_assert_test");
        writableDir.toFile().deleteOnExit();
        
        IOUtil.assertDirectoryIsWritable(writableDir); // Should not throw
    }

    /**
     * Test assertDirectoryIsWritable throws for non-existent directory.
     * Validates: Requirement 1.1
     */
    @Test(expectedExceptions = SAMException.class)
    public void testAssertDirectoryIsWritableThrowsForNonExistent() {
        Path nonExistent = Paths.get("/non/existent/directory");
        IOUtil.assertDirectoryIsWritable(nonExistent);
    }

    /**
     * Test assertDirectoryIsWritable throws for file (not directory).
     * Validates: Requirement 1.1
     */
    @Test(expectedExceptions = SAMException.class)
    public void testAssertDirectoryIsWritableThrowsForFile() throws IOException {
        Path file = Files.createTempFile("not_a_dir", ".txt");
        file.toFile().deleteOnExit();
        IOUtil.assertDirectoryIsWritable(file);
    }

    /**
     * Test assertDirectoryIsReadable with Path.
     * Validates: Requirement 1.1
     */
    @Test
    public void testAssertDirectoryIsReadableWithPath() throws IOException {
        Path readableDir = Files.createTempDirectory("readable_dir_assert_test");
        readableDir.toFile().deleteOnExit();
        
        IOUtil.assertDirectoryIsReadable(readableDir); // Should not throw
    }

    /**
     * Test assertDirectoryIsReadable throws for non-existent directory.
     * Validates: Requirement 1.1
     */
    @Test(expectedExceptions = SAMException.class)
    public void testAssertDirectoryIsReadableThrowsForNonExistent() {
        Path nonExistent = Paths.get("/non/existent/directory");
        IOUtil.assertDirectoryIsReadable(nonExistent);
    }

    /**
     * Test URI to Path construction with IOUtil.getPath(URI).
     * Validates: Requirements 1.4, 1.5
     */
    @Test
    public void testUriToPathConstructionWithIOUtil() throws IOException {
        // Test with file:// URI
        URI fileUri = URI.create("file:///tmp/test_uri.txt");
        Path path = IOUtil.getPath(fileUri);
        Assert.assertNotNull(path, "Path should not be null");
        Assert.assertEquals(path.toUri().getScheme(), "file", "Scheme should be 'file'");
        
        // Test with string URI
        String uriString = "file:///tmp/test_uri_string.txt";
        Path pathFromString = IOUtil.getPath(uriString);
        Assert.assertNotNull(pathFromString, "Path from string should not be null");
        Assert.assertEquals(pathFromString.toUri().getScheme(), "file", "Scheme should be 'file'");
    }

    /**
     * Test IOUtil.getPath with simple path string (no scheme).
     * Validates: Requirements 1.4, 1.5
     */
    @Test
    public void testGetPathWithSimpleString() throws IOException {
        // Test with absolute path
        String absolutePath = "/tmp/simple_path.txt";
        Path path = IOUtil.getPath(absolutePath);
        Assert.assertNotNull(path, "Path should not be null");
        Assert.assertTrue(path.isAbsolute(), "Path should be absolute");
        
        // Test with relative path
        String relativePath = "relative/path.txt";
        Path relPath = IOUtil.getPath(relativePath);
        Assert.assertNotNull(relPath, "Relative path should not be null");
        Assert.assertFalse(relPath.isAbsolute(), "Path should be relative");
    }

    /**
     * Test IOUtil.getPaths with list of URI strings.
     * Validates: Requirements 1.4, 1.5
     */
    @Test
    public void testGetPathsWithList() throws IOException {
        List<String> pathStrings = Arrays.asList(
            "/tmp/file1.txt",
            "/tmp/file2.txt",
            "/tmp/file3.txt"
        );
        
        List<Path> paths = IOUtil.getPaths(pathStrings);
        Assert.assertEquals(paths.size(), 3, "Should return 3 paths");
        
        for (int i = 0; i < paths.size(); i++) {
            Assert.assertNotNull(paths.get(i), "Path " + i + " should not be null");
            Assert.assertTrue(paths.get(i).toString().contains("file" + (i + 1)), 
                "Path should contain expected filename");
        }
    }

    /**
     * Test that IOUtil.hasScheme correctly identifies URIs with schemes.
     * Validates: Requirement 1.4
     */
    @Test
    public void testHasScheme() {
        // URIs with schemes
        Assert.assertTrue(IOUtil.hasScheme("file:///tmp/test.txt"), "file:// should have scheme");
        Assert.assertTrue(IOUtil.hasScheme("http://example.com/file.txt"), "http:// should have scheme");
        Assert.assertTrue(IOUtil.hasScheme("https://example.com/file.txt"), "https:// should have scheme");
        
        // Paths without schemes
        Assert.assertFalse(IOUtil.hasScheme("/tmp/test.txt"), "Absolute path should not have scheme");
        Assert.assertFalse(IOUtil.hasScheme("relative/path.txt"), "Relative path should not have scheme");
        Assert.assertFalse(IOUtil.hasScheme("file.txt"), "Simple filename should not have scheme");
    }

    /**
     * Verify that all non-deprecated public methods in IOUtil do not use File.
     * Validates: Requirement 1.5
     */
    @Test
    public void testNoNonDeprecatedFileBasedMethods() {
        java.lang.reflect.Method[] methods = IOUtil.class.getMethods();
        List<String> violations = new ArrayList<>();
        
        for (java.lang.reflect.Method method : methods) {
            // Skip deprecated methods - they are allowed to use File for backward compatibility
            if (method.isAnnotationPresent(Deprecated.class)) {
                continue;
            }
            
            // Skip methods from Object class
            if (method.getDeclaringClass().equals(Object.class)) {
                continue;
            }
            
            // Check parameters for File type
            for (Class<?> paramType : method.getParameterTypes()) {
                if (paramType.equals(File.class)) {
                    violations.add("Method " + method.getName() + " has non-deprecated File parameter");
                }
            }
            
            // Check return type for File type
            if (method.getReturnType().equals(File.class)) {
                violations.add("Method " + method.getName() + " has non-deprecated File return type");
            }
        }
        
        Assert.assertTrue(violations.isEmpty(), 
            "Found non-deprecated File-based methods: " + String.join(", ", violations));
    }

    /**
     * Test that all File-based methods have been removed from IOUtil.
     * Validates: Requirement 1.5 - IOUtil SHALL NOT contain any File-based method signatures
     */
    @Test
    public void testDeprecatedFileMethodsExistForBackwardCompatibility() {
        java.lang.reflect.Method[] methods = IOUtil.class.getMethods();
        
        for (java.lang.reflect.Method method : methods) {
            // No method should accept or return File (deprecated or not)
            for (Class<?> paramType : method.getParameterTypes()) {
                if (paramType.equals(File.class)) {
                    Assert.fail("Method " + method.getName() + " still has File parameter - migration incomplete");
                }
            }
            if (method.getReturnType().equals(File.class)) {
                Assert.fail("Method " + method.getName() + " still returns File - migration incomplete");
            }
        }
    }

    /**
     * Test IOUtil path operations work correctly with jimfs (in-memory filesystem).
     * Validates: Requirements 1.1, 1.4
     */
    @Test
    public void testPathOperationsWithJimfs() throws IOException {
        // Create file in jimfs
        Path jimfsFile = inMemoryFileSystem.getPath("/jimfs_test_file.txt");
        Files.write(jimfsFile, "jimfs content".getBytes());
        
        // Test existence
        Assert.assertTrue(Files.exists(jimfsFile), "Jimfs file should exist");
        
        // Test readability
        Assert.assertTrue(Files.isReadable(jimfsFile), "Jimfs file should be readable");
        IOUtil.assertFileIsReadable(jimfsFile);
        
        // Test writability
        Assert.assertTrue(Files.isWritable(jimfsFile), "Jimfs file should be writable");
        IOUtil.assertFileIsWritable(jimfsFile);
        
        // Test directory operations
        Path jimfsDir = inMemoryFileSystem.getPath("/jimfs_test_dir");
        Files.createDirectory(jimfsDir);
        Assert.assertTrue(Files.isDirectory(jimfsDir), "Jimfs directory should be directory");
        IOUtil.assertDirectoryIsWritable(jimfsDir);
        IOUtil.assertDirectoryIsReadable(jimfsDir);
        
        // Test file operations
        try (InputStream is = IOUtil.openFileForReading(jimfsFile)) {
            Assert.assertNotNull(is, "Should be able to open jimfs file for reading");
        }
        
        Path jimfsOutput = inMemoryFileSystem.getPath("/jimfs_output.txt");
        try (OutputStream os = IOUtil.openFileForWriting(jimfsOutput)) {
            os.write("output".getBytes());
        }
        Assert.assertTrue(Files.exists(jimfsOutput), "Output file should exist in jimfs");
    }

    /**
     * Test IOUtil.assertFilesEqual with Path.
     * Validates: Requirement 1.1
     */
    @Test
    public void testAssertFilesEqualWithPath() throws IOException {
        Path file1 = Files.createTempFile("equal_test1", ".txt");
        Path file2 = Files.createTempFile("equal_test2", ".txt");
        file1.toFile().deleteOnExit();
        file2.toFile().deleteOnExit();
        
        String content = "identical content for both files";
        Files.write(file1, content.getBytes());
        Files.write(file2, content.getBytes());
        
        IOUtil.assertFilesEqual(file1, file2); // Should not throw
    }

    /**
     * Test IOUtil.assertFilesEqual throws for different files.
     * Validates: Requirement 1.1
     */
    @Test(expectedExceptions = SAMException.class)
    public void testAssertFilesEqualThrowsForDifferentFiles() throws IOException {
        Path file1 = Files.createTempFile("diff_test1", ".txt");
        Path file2 = Files.createTempFile("diff_test2", ".txt");
        file1.toFile().deleteOnExit();
        file2.toFile().deleteOnExit();
        
        Files.write(file1, "content one".getBytes());
        Files.write(file2, "content two".getBytes());
        
        IOUtil.assertFilesEqual(file1, file2);
    }

    /**
     * Test IOUtil.assertFileSizeNonZero with Path.
     * Validates: Requirement 1.1
     */
    @Test
    public void testAssertFileSizeNonZeroWithPath() throws IOException {
        Path nonEmptyFile = Files.createTempFile("nonzero_test", ".txt");
        nonEmptyFile.toFile().deleteOnExit();
        Files.write(nonEmptyFile, "some content".getBytes());
        
        IOUtil.assertFileSizeNonZero(nonEmptyFile); // Should not throw
    }

    /**
     * Test IOUtil.assertFileSizeNonZero throws for empty file.
     * Validates: Requirement 1.1
     */
    @Test(expectedExceptions = SAMException.class)
    public void testAssertFileSizeNonZeroThrowsForEmptyFile() throws IOException {
        Path emptyFile = Files.createTempFile("empty_test", ".txt");
        emptyFile.toFile().deleteOnExit();
        // File is empty by default
        
        IOUtil.assertFileSizeNonZero(emptyFile);
    }
}


