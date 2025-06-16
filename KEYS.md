# GPG Key for CI/CD Signing

Used for signing Maven Central releases. You should verify the artifact signatures using this public key before trusting any published release.

- Key ID: `0x06C84BD97E83C4FD`
- Fingerprint: `FDDE 0EF7 B130 2B9E C17B  D3C9 06C8 4BD9 7E83 C4FD`
- UID: `Peak Solution CI Signing Key <ci@peak-solution.de>`
- Keyserver: `keys.openpgp.org`

To import the key:
```
gpg --keyserver keys.openpgp.org --recv-keys 0x06C84BD97E83C4FD
```

# GPG Keys of Maintainers

This project publishes signed artifacts (e.g., `.jar.asc`, `.pom.asc`) to Maven Central.
Below is the list of GPG keys used by the maintainers to sign these artifacts.

| Name          | Email                     | Key ID               | Fingerprint                                 | Keyserver             |
|---------------|---------------------------|----------------------|---------------------------------------------|------------------------|
| Markus Renner | m.renner@peak-solution.de | `0x68E9713E911A871C` | `A40F AA78 F7AB D88D 254E D696 68E9 713E 911A 871C` | `keys.openpgp.org` |
|    |            | ``                   | `` | `keys.openpgp.org` |

## Importing the Keys

```
gpg --keyserver hkps://keys.openpgp.org --recv-keys 0x68E9713E911A871C
