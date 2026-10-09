# openATFX project instructions

## Project and layout

openATFX is a Java 17 Maven library for reading and writing ASAM ODS ATFX
files. It exposes a direct Java API and a CORBA OO-API backed by that Java
API. Preserve compatibility across both interfaces.

- `src\main\java\com\peaksolution\openatfx\api`: Java API, instance cache,
  ATFX parsing and value access.
- `src\main\java\com\peaksolution\openatfx\io`: file and binary I/O.
- `src\main\java\com\peaksolution\openatfx\basestructure`: base-model support.
- `src\main\java\com\peaksolution\openatfx\main`: executable entry point.
- `src\main\xsd`: JAXB schemas; Maven generates sources during the build.
- `src\test\java` and `src\test\resources`: tests and ATFX/binary fixtures.
- `pom.xml`: dependencies, build plugins and optional profiles.

Consult `README.md` for public API usage and context properties. Glassfish
CORBA dependencies have provided scope; do not remove them merely because
a change concerns only the Java API.

## Build and test

Use JDK 17 and Maven from the repository root. There is no Maven wrapper.

- Targeted regression tests: `mvn "-Dtest=LazyLocalColumnLoadingTest" test`
- Related tests in one invocation:
  `mvn "-Dtest=AtfxReaderTest,ValueMatrixReadFlagsTest,ExtCompReaderTest" test`
- All tests: `mvn test`
- CI build, including license checks: `mvn clean verify -B -P license`

Prefer tests covering the changed behavior before a full build. Tests use
JUnit Jupiter and AssertJ; the build also supports existing JUnit Vintage
tests. Reuse the existing test framework and fixtures.

Do not activate the `release`, `dependency-track` or `owasp` profiles for
routine local validation: these involve publishing, external services or
credential-dependent operations. Do not introduce credentials into source
or configuration.

## Implementation conventions

Follow the formatting and import style of the file being edited. Reuse
existing datamodel types, attribute lookup helpers and file-handler
abstractions. Keep fixes scoped to the requested behavior and add regression
coverage for behavioral changes. Do not manually edit generated sources or
build output under `target`.

LocalColumn binary data can be expensive to materialize. Preserve deferred
loading of `AoLocalColumn` values and flags during bulk instance value
enumeration. Explicit attribute queries must still retrieve the data on
demand, cache it on the owning instance, and make loaded data available to
subsequent bulk reads. Cover access by application attribute name, attribute
number and base name when changing this behavior.

Maintain compatibility with ATFX files whose application attribute names
differ from their base names. Preserve binary value/flag semantics and
external-component path handling. Surface I/O and API errors using existing
project mechanisms rather than silently returning empty or successful data.

## Workspace safety

Inspect the working tree before editing and preserve pre-existing changes,
including untracked tests. Do not commit, discard changes or modify IDE
workspace settings unless requested. Keep local session transcripts,
machine-specific configuration and credentials out of the repository.
