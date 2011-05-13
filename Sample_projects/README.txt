1. Copy the attached Maven artifact (from the 'MavenArtifacts' directory) to your local Maven repo.
2. Run 'mvn install'; This will fail as many tests are designed to fail; The important thing is that it will download required artifacts.
3. Import to your IDE (for Eclipse first run 'mvn eclipse:eclipse')
4. Play with UsesDirectivePositiveTest, UsesDirectiveConflictTest, TclTest, and SpiTest JUnit tests

Remember - some tests will always fail!

Have fun!