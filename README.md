# openATFX Java Library

openATFX is an openSource Java library for reading and writing [ASAM ODS](https://www.asam.net/standards/detail/ods/) standardized .atfx files.

openATFX 1.x was previously published on [SourceForge](https://sourceforge.net/projects/openatfx/). Further development has been taken up by [Peak Solution GmbH](https://www.peak-solution.de) and deployment is now done via the company's [GitHub space](https://github.com/peak-solution/openatfx).

Compared to openATFX 1.x, version 2.x adds a Java API to openATFX and refactors the CORBA OO-API to work internally with that Java API. Connecting via the OO-API should be backward compatible for clients previously working with previous versions of openATFX.

The direct usage of the new Java API is already possible, but it may undergo further development in the future. This should be mostly some new Java API methods to make working with the API easier and more convenient, as more clients will adopt the new Java API. Breaking changes in the API are not intended, but cannot be ruled out at the moment. If any should be necessary they will be as limited as possible, though, and explicitly communicated.

## License

This project is licensed under the terms of the [Apache License, Version 2.0](LICENSE).

## Configuration

openATFX uses context properties to adjust its configuration. In the case of ODS session context (CORBA OO-API) several of the standard-defined properties can be set. Additionally and also for the Java API use, following properties specifically define behaviour of openATFX:

- INDENT_XML:
  Whether to indent the XML on output. Possible values are "TRUE" or "FALSE", default is true.
- MAX_CONDITION_RELATION_FOLLOW:
  The maximum number of relation jumps checked to identify a related condition
  instance in calls to getInstances, default value is 3.
- WRITE_EXTERNALCOMPONENTS:
  Whether to write measurement data specifically as external components. Otherwise they may
  be written into normal component files, which resemble the ODS write mode "database" in atfx
  context. Possible values are "TRUE" or "FALSE", default is false.
- EXTENDED_COMPATIBILITYMODE:
  Whether to switch on the compatibility mode for more tolerance on read atfx file, regarding
  deviations or violations of the ODS specification. Possible values are "TRUE" or "FALSE",
  default is false.
- EXTCOMP_FILENAME_STRIP_STRING:
  If provided, strips the given String from the beginning of external component (flags-)filename
  urls. This is helpful, if for example an atfx provider does not remove his internal file symbol
  from those file paths.
- TRIM_STRING_VALUES:
  If set to true, will remove leading and trailing whitespaces of read String values
  (no sequences, yet) from atfx file and the same before writing any String values to an atfx file.

## Eclipse Glassfish ORB Dependency

It is required to use the Eclipse Glassfish ORB with openATFX 2.x! Other previously used ORBs like JacORB or SUN Java ORB are not supported anymore. This decision was made because of those ORBs not being properly maintained anymore. To prevent the release artifact from becoming too big by including the ORB and all its dependencies, however, the relevant artifacts are specified as "provided" in the Maven pom.xml. You have to provide the libraries in your environment when using openATFX. See the pom.xml of openATFX for details on what is required.

You also have to provide this dependency if your are only using the Java API of openATFX! So far, the CORBA API has not yet been extracted into a separate module, but this is planned as a future improvement.

## Use of CORBA OO-API

The specification of the OO-API can be found in the ASAM ODS standard document. You may need to be an ASAM member to be able to access the specification document!

Code example:

```
public static void main(String[] args) {
    try {
        ORB orb = ORB.init(new String[0], System.getProperties());
        AoFactory aoFactory = AoServiceFactory.getInstance().newAoFactory(orb);
        AoSession aoSession = aoFactory.newSession("FILENAME=" + new File("C:/file.atfx"));
    } catch (AoException e) {
        System.err.println(e.reason);
    }
}
```

## Use of Java API
The Java API is made ready to the degree, that it works as the main data access layer called via the CORBA OO-API. While already being available and usable directly by clients to avoid all the CORBA overhead, the documentation and also the functionality may not be complete, yet. You are welcome to use it and provide any feedback that helps improve it further!

Code example:

```
public static void main(String[] args) {
	Path atfxFile = Paths.get("C:/file.atfx");
	OpenAtfx atfx = new OpenAtfx();
	try {
		OpenAtfxAPI api = atfx.openFile(atfxFile);
	} catch (OpenAtfxException e) {
		System.err.println(e.getMessage());
	}
}
```

## GPG Key

This key is used for signing of releases:
**Key ID**: `0x06C84BD97E83C4FD`  
**Fingerprint**: `FDDE 0EF7 B130 2B9E C17B  D3C9 06C8 4BD9 7E83 C4FD`  
**Keyserver**: `https://keys.openpgp.org`
