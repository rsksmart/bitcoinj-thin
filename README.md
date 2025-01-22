[![CodeQL](https://github.com/rsksmart/bitcoinj-thin/workflows/CodeQL/badge.svg)](https://github.com/rsksmart/bitcoinj-thin/actions?query=workflow%3ACodeQL)
[![OpenSSF Scorecard](https://api.scorecard.dev/projects/github.com/rsksmart/bitcoinj-thin/badge)](https://scorecard.dev/viewer/?uri=github.com/rsksmart/bitcoinj-thin)


### Welcome to bitcoinj

The bitcoinj library is a Java implementation of the Bitcoin protocol, which allows it to maintain a wallet and send/receive transactions without needing a local copy of Bitcoin Core. It comes with full documentation and some example apps showing how to use it.
RSK made a "thin" version of bitcoinj including just the components needed by the RSK node.

### Technologies

* Java 6 for the core modules, Java 8 for everything else
* [Maven 3+](http://maven.apache.org) - for building the project

### Getting started

To get started, it is best to have the latest JDK and Maven installed. The HEAD of the `master` branch contains the latest development code and various production releases are provided on feature branches.

#### Building from the command line

To perform a full build use
```
mvn clean package
```

The outputs are under the `target` directory.

#### Building from an IDE

Alternatively, just import the project using your IDE. [IntelliJ](http://www.jetbrains.com/idea/download/) has Maven integration built-in and has a free Community Edition. Simply use `File | Import Project` and locate the `pom.xml` in the root of the cloned project source tree.

### Where next?

Now you are ready to [follow the tutorial](https://bitcoinj.github.io/getting-started).
