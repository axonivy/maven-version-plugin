# Maven Version Plugin

This Maven plugin is used to raise the versin in `pom.xml`, `MANIFEST.MF` and other OSGi artifacts.

## Releasing

- Remove `-SNAPSHOT` from `pom.xml`
- Push & Build
- Raise version and add `-SNAPSHOT` to `pom.xml`
- Push
