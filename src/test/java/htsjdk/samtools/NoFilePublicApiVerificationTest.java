package htsjdk.samtools;

import htsjdk.HtsjdkTest;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Verifies that no public methods in the main source accept or return java.io.File
 * (excluding deprecated methods).
 * This test ensures the File-to-Path migration is complete.
 * 
 * Requirements: 12.2, 12.3, 12.4
 */
public class NoFilePublicApiVerificationTest extends HtsjdkTest {

    @Test
    public void testIOUtilHasNoFileParameters() {
        assertNoFileInPublicApi(htsjdk.samtools.util.IOUtil.class);
    }

    @Test
    public void testSamReaderFactoryHasNoFileParameters() {
        assertNoFileInPublicApi(htsjdk.samtools.SamReaderFactory.class);
    }

    @Test
    public void testSAMFileWriterFactoryHasNoFileParameters() {
        assertNoFileInPublicApi(htsjdk.samtools.SAMFileWriterFactory.class);
    }

    @Test
    public void testSamInputResourceHasNoNonDeprecatedFileMethods() {
        assertNoFileInPublicApi(htsjdk.samtools.SamInputResource.class);
    }

    private void assertNoFileInPublicApi(Class<?> clazz) {
        List<String> violations = new ArrayList<>();
        
        for (Method method : clazz.getDeclaredMethods()) {
            if (!Modifier.isPublic(method.getModifiers())) continue;
            if (method.isAnnotationPresent(Deprecated.class)) continue;
            
            // Check parameters
            for (Class<?> paramType : method.getParameterTypes()) {
                if (paramType == File.class) {
                    violations.add(method.getName() + " accepts File parameter");
                }
            }
            
            // Check return type
            if (method.getReturnType() == File.class) {
                violations.add(method.getName() + " returns File");
            }
        }
        
        Assert.assertTrue(violations.isEmpty(),
            clazz.getSimpleName() + " should not have non-deprecated File-based methods: " + violations);
    }
}
