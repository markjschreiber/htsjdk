# Technology Stack

## Build System

- **Gradle** (version 8.5) with wrapper script included
- Build file: `build.gradle`
- Use `./gradlew` (Unix/macOS) for all build commands

## Language & Runtime

- **Java 17** (toolchain configured)
- Targets Java 8 compatibility for releases
- Tested on Java 8 and 11

## Key Dependencies

### Core Libraries
- `commons-logging:commons-logging` - Logging
- `org.xerial.snappy:snappy-java` - Compression
- `org.apache.commons:commons-compress` - Archive handling
- `org.tukaani:xz` - XZ compression
- `org.json:json` - JSON processing
- `org.openjdk.nashorn:nashorn-core` - JavaScript engine
- `gov.nih.nlm.ncbi:ngs-java` - SRA format support
- `org.apache.commons:commons-jexl` - Expression language

### Test Dependencies
- **TestNG** (not JUnit) - Primary testing framework
- `com.google.jimfs:jimfs` - In-memory file system for testing
- `com.google.guava:guava` - Utilities
- `org.apache.commons:commons-lang3` - String/utility functions

## Common Commands

### Build & Compile
```bash
./gradlew                    # Compile and build JAR
./gradlew jar                # Build JAR explicitly
./gradlew clean              # Clean build artifacts
./gradlew shadowJar          # Build monolithic JAR with dependencies
```

### Testing
```bash
./gradlew test               # Run main test suite (excludes slow, broken, external)
./gradlew test --tests ClassName          # Run specific test class
./gradlew test --tests ClassName --debug-jvm  # Debug specific test
./gradlew testFTP            # Run FTP tests
./gradlew testExternalApis   # Run SRA, ENA, HTTP tests
./gradlew jacocoTestReport   # Generate coverage report
```

### Publishing
```bash
./gradlew install            # Install to local Maven repository
./gradlew publishToMavenLocal  # Publish snapshot locally
```

### Other
```bash
./gradlew tasks              # List all available tasks
./gradlew spotbugsMain       # Run static analysis
```

## Test Configuration

- Tests use **TestNG** with data providers
- Test groups: `slow`, `broken`, `defaultReference`, `ftp`, `http`, `sra`, `ena`, `unix`
- Default test run excludes: slow, broken, external API tests
- Tests run serially (maxParallelForks = 1) due to threading issues
- Test resources in `src/test/resources/htsjdk/`

## Code Quality Tools

- **SpotBugs** for static analysis (high priority issues only)
- **JaCoCo** for code coverage
- Exclude filter: `gradle/spotbugs-exclude.xml`
