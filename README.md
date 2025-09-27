# data-package-tools

Data package software bill of materials (SBOM) tools

## Hacking data-package-tools

Install

 * JDK 17 or later, https://openjdk.java.net
 * Apache Maven 3.6.3 or later, https://maven.apache.org

To build
```bash
$ mvn package

$ export PATH=$PATH:`pwd`/target/appassembler/bin
```

## Using data-package-tools

### Usage

```bash
$ dpt --help
USAGE
  dpt [-hV] [COMMAND]

Data package software bill of materials (SBOM) tools.

OPTIONS
  -h, --help      Show this help message and exit.
  -V, --version   Print version information and exit.

COMMANDS
  generate             Generate a data package software bill of materials (SBOM).
  validate             Validate a data package software bill of materials (SBOM).
  help                 Display help information about the specified command.
  generate-completion  Generate bash/zsh completion script for dpt.
```

### Generate command

Generate a data package software bill of materials (SBOM).

```bash
$ dpt generate --help
USAGE
  dpt generate [-hV] [--verbose] -a=<agent> [-o=<outputPath>] -p=<prefix> [-s=<suffix>] [<inputPaths>...]

Generate a data package software bill of materials (SBOM).

PARAMETERS
      [<inputPaths>...]

OPTIONS
      --verbose                    Show additional logging messages.
  -a, --agent=<agent>              Agent name, required.
  -p, --prefix=<prefix>            Identifier URI prefix, required.
  -s, --suffix=<suffix>            Optional identifier suffix, if any.
  -o, --output-path=<outputPath>   Output path, default stdout.
  -h, --help                       Show this help message and exit.
  -V, --version                    Print version information and exit.
```

### Validate command

Validate a data package software bill of materials (SBOM).

```bash
$ dpt validate --help
USAGE
  dpt validate [-hV] [--verbose] [<inputPaths>...]

Validate a data package software bill of materials (SBOM).

PARAMETERS
      [<inputPaths>...]

OPTIONS
      --verbose           Show additional logging messages.
  -h, --help              Show this help message and exit.
  -V, --version           Print version information and exit.
```
