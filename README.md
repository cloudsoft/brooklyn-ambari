org.apache.brooklyn.brooklyn-ambari
===

## Opening in an IDE

To open this project in an IDE, you will need maven support enabled
(e.g. with the relevant plugin).  You should then be able to develop
it and run it as usual.  For more information on IDE support, visit:

    http://brooklyncentral.github.io/dev/build/ide.html


## Sample Blueprint

Below is a sample YAML blueprint that can be used to deploy Apache Ambari
to AWS, adding the list of services shown:
 
    location:
      aws-ec2:us-east-1:
        minRam: 8192
        osFamily: ubuntu
        osVersionRegex: 12.*

    services:
    - type: io.brooklyn.ambari.AmbariCluster
      name: Ambari Cluster
      brooklyn.config:
        securityGroup: test-ambari
        initialSize: 3
        services:
        - FALCON
        - FLUME
        - GANGLIA
        - HBASE
        - HDFS
        - KAFKA
        - KERBEROS
        - MAPREDUCE2
        - NAGIOS
        - OOZIE
        - PIG
        - SLIDER
        - SQOOP
        - STORM
        - TEZ
        - YARN
        - ZOOKEEPER


## Building and Running

There are several options available for building and running.

### Building a standlone distro

To build an assembly, simply run:

    mvn clean assembly:assembly

This creates a tarball with a full standalone application which can be installed in any *nix machine at:
    target/org.apache.brooklyn.brooklyn-ambari-1.0-SNAPSHOT-dist.tar.gz

It also installs an unpacked version which you can run locally:
 
     cd target/org.apache.brooklyn.brooklyn-ambari-0.1-SNAPSHOT-dist/org.apache.brooklyn.brooklyn-ambari-1.0-SNAPSHOT
     ./start.sh launch

For more information see the README (or `./start.sh help`) in that directory.

To configure cloud and fixed-IP locations, see the README file in the built application directly.
For more information you can run `./start.sh help`) in that directory.


### Adding to Brooklyn dropins

An alternative is to build a single jar and to add that to an existing Brooklyn install.

First install Brooklyn. There are instructions at https://brooklyn.incubator.apache.org/v/latest/start/index.html

Then simply run:

    mvn clean install

You can copy the jar to your Brooklyn dropins folder, and then launch Brooklyn:

    cp target/brooklyn-ambari-0.1-SNAPSHOT.jar $BROOKLYN_HOME/lib/dropins/
    nohup $BROOKLYN_HOME/bin/brooklyn launch &


### Adding to Brooklyn catalog on-the-fly

*TODO: this is work in progress; the project is still to be converted to build an OSGi bundle*

A third alternative is to build an OSGi bundle, which can then be deployed to 
a running Brooklyn server. The Ambari blueprint can be added to the catalog
(referencing the required OSGi bundle), which makes the blueprint available
to Brooklyn users.

General instructions for how to do this are available at:
https://brooklyn.incubator.apache.org/v/latest/ops/catalog/index.html

First create the OSGi bundle:

    mvn clean install

Copy the OSGi bundle to a stable location. This could be something like Artifactory, or
for test purposes it could be just on your local file system:

    cp target/brooklyn-ambari-0.1-SNAPSHOT.jar /path/to/artifacts/brooklyn-ambari-0.1-SNAPSHOT.jar

Add the AmbariCluster to the catalog. Here we assume Brooklyn is running at https://localhost:8443,
with credentials admin:password.

First create the catalog definition, which is a YAML file (assumed here to be at `/path/to/ambari-catalog.yaml`):

    brooklyn.catalog:
      id: io.brooklyn.ambari.AmbariCluster
      version: 0.1-SNAPSHOT
      iconUrl: http://ambari.apache.org/images/apache-ambari-project.jpg
      description: Apache Ambari Cluster
      libraries:
      - url: file:///path/to/artifacts/brooklyn-ambari-0.1-SNAPSHOT.jar

    services:
    - type: io.brooklyn.ambari.AmbariCluster
      name: Ambari Cluster

Upload this to the Brooklyn server:

    curl https://127.0.0.1:8443/v1/catalog --data-binary @/path/to/ambari-catalog.yaml

Users can then provision an Ambari cluster using the YAML shown in the first section.
