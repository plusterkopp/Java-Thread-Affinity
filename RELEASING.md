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

`deploy` builds, signs (gpg asks for the passphrase) and uploads. Then publish the deployment at
https://central.sonatype.com/publishing/deployments; it shows up on Maven Central some minutes later.
Published releases cannot be changed or deleted, so a broken release needs a new version.
If the upload fails validation, fix the problem, move the tag (`git tag -f -a vX.Y.Z ...`) and deploy again.

Finally push, and start the next version:

```bash
git push origin master vX.Y.Z
mvn versions:set -DnewVersion=X.Y.(Z+1)-SNAPSHOT
git commit -am "Next development version"
git push origin master
```
