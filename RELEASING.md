# Releasing

Releases go to Maven Central through the Sonatype Central Portal. Use Maven 3.9; Sonatype's publishing plugin
does not work with Maven 4 yet.

## One-time setup

1. **Portal account:** sign in at https://central.sonatype.com. Old OSSRH accounts were migrated there,
   so the old OSSRH username and password should work.
2. **Namespace `de.pkmd`:** open *View Namespaces* (user menu, top right) and check whether `de.pkmd` is listed,
   possibly under "OSSRH Namespaces". If it isn't:
   *Add Namespace* → `de.pkmd` → copy the verification key → add it as a TXT record to `pkmd.de` at the DNS host →
   wait until `dig +short -t txt pkmd.de` shows it → only then click *Verify Namespace*.
3. **User token:** https://central.sonatype.com/usertoken → *Generate User Token*. It is shown only once.
   Put it into `~/.m2/settings.xml`:

   ```xml
   <settings>
     <servers>
       <server>
         <id>central</id>
         <username>TOKEN-USERNAME</username>
         <password>TOKEN-PASSWORD</password>
       </server>
     </servers>
   </settings>
   ```
4. **GPG key:** Central wants every file signed by the key's primary key, and the public key on a keyserver:

   ```bash
   gpg --quick-gen-key "Ralf Helbing <EMAIL>" rsa4096 sign 3y
   gpg --keyserver keyserver.ubuntu.com --send-keys KEYID
   ```

   With several secret keys in the keyring, tell the gpg plugin which one to use, in `~/.m2/settings.xml`:

   ```xml
   <profiles>
     <profile>
       <id>gpg</id>
       <properties>
         <gpg.keyname>FINGERPRINT</gpg.keyname>
       </properties>
     </profile>
   </profiles>
   <activeProfiles>
     <activeProfile>gpg</activeProfile>
   </activeProfiles>
   ```

   Sonatype's documentation only shows RSA keys. If the Portal rejects the signatures of an Ed25519 key,
   nothing gets published; create an RSA key as above and deploy again.

## Checking other machines

To run the unit tests on a machine that has Java but no Maven, build a self-contained jar and copy it there:

```bash
mvn -P check package -DskipTests
```

```bash
java -jar affinity-X.Y.Z-check.jar > layout-check.txt 2>&1
```

It prints the Java version, the detected layout and the raw layout data, then the result of every test class.
On Java 24+, add `--enable-native-access=ALL-UNNAMED` to silence JNA's warnings.

## Release X.Y.Z

```bash
mvn versions:set -DnewVersion=X.Y.Z
```

Update the version in README.md's dependency snippet, then:

```bash
git commit -am "Release X.Y.Z"
git tag -a vX.Y.Z -m "Release X.Y.Z"
mvn -P release clean deploy
```

`deploy` builds, signs (gpg asks for the passphrase) and uploads. Don't add `-B`: in batch mode the gpg plugin
forbids passphrase prompts, and signing fails with "No pinentry". Then publish the deployment at
https://central.sonatype.com/publishing/deployments. Publishing cannot be cancelled once started; the Portal shows
"Publishing" until the release has reached Maven Central, which can take from minutes to hours.
Published releases cannot be changed or deleted, so a broken release needs a new version.
If the upload fails validation, fix the problem, move the tag (`git tag -f -a vX.Y.Z ...`) and deploy again.

Finally push, and start the next version:

```bash
git push origin master vX.Y.Z
mvn versions:set -DnewVersion=X.Y.(Z+1)-SNAPSHOT
git commit -am "Next development version"
git push origin master
```
