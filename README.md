Brooklyn Ambari
===

This project contains [Apache Brooklyn](https://brooklyn.apache.org/)
blueprints for [Apache Ambari](https://ambari.apache.org) servers and for deploying
Ambari stacks to those servers.


[![Build Status](https://api.travis-ci.org/brooklyncentral/brooklyn-ambari.svg?branch=master)](https://travis-ci.org/brooklyncentral/brooklyn-ambari)

This blueprint currently uses the following dependencies:

- Apache Ambari **2.2.0.0**
- HDP stack definition **2.3**


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
      # This security group needs to have port 8080 open
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

To use the blueprint unchanged first create an AWS security group called "test-ambari" with
an inbound TCP rule for port 8080 and allowing all traffic within the security group.


## Building and Running

There are several options available for building and running.

### Karaf

If you are using Apache Brooklyn in karaf mode then you can simply use the cli with the following command:

    br add-catalog catalog.bom

This will add brooklyn-ambari and a simple big data blueprint (ambari-cluster-application) to your Apache Brooklyn instance.

### Building a standlone distro

To build an assembly, simply run:

    mvn clean install

This creates a tarball with a full standalone application which can be installed in any *nix machine at:
    dist/target/brooklyn-ambari-dist.tar.gz

It also installs an unpacked version which you can run locally:
 
     cd dist/target/brooklyn-ambari-dist/brooklyn-ambari/
     ./start.sh launch -l <location> --ambari

For more information see the README (or `./start.sh help`) in that directory.

To configure cloud and fixed-IP locations, see the README file in the built application directly.
For more information you can run `./start.sh help`) in that directory.


### Adding to Brooklyn dropins

An alternative is to build a single jar and to add that to an existing Brooklyn install.

First install Brooklyn. There are instructions at https://brooklyn.incubator.apache.org/v/latest/start/index.html

Then simply run:

    mvn clean install

You can copy the jar to your Brooklyn dropins folder, and then launch Brooklyn:

    cp ambari/target/brooklyn-ambari-0.1-SNAPSHOT.jar $BROOKLYN_HOME/lib/dropins/
    nohup $BROOKLYN_HOME/bin/brooklyn launch &


### Adding to Brooklyn catalog on-the-fly

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

    cp ambari/target/brooklyn-ambari-0.1-SNAPSHOT.jar /path/to/artifacts/brooklyn-ambari-0.1-SNAPSHOT.jar

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

## Extra services

### Usage

To add an extra service to the cluster (i.e. not fully supported by the default stack, such as [Apache Ranger](http://ranger.incubator.apache.org/)
or [Apache Spark](http://spark.apache.org/)) the new configuration key `extraServices` can be used:

    ...
    services:
    - type: io.brooklyn.ambari.AmbariCluster
      brooklyn.children:
      - type: io.brooklyn.ambari.service.Ranger
        brooklyn.config:
          bindTo: NameNode
          serviceName: RANGER
          componentNames:
          - RANGER_ADMIN
          - RANGER_USERSYNC
      ...

An extra service has three configuration keys:

- `bindTo`: use to specify where the components will be bind to by default. This is **optional** is you use the binding
 mechanism of `componentNames` configuration key. This is **not needed** if you have a service based deployment.
- `serviceName`: use to specify the name of the extra service to install. This is **required** if you have a services
 based deployment.
- `componentNames`: use to specify the names of the extra service's components. This is **required** if you have a host
 group based deployment. Foir each entry, you can pass only the component name (as the above example) or the mapping of
 component name <-> host group name like so: `<COMPONENT_NAME>|<HOST_GROUP_NAME>`. If there is no mapping specify, the
 component will be bind to the host group specify by the configuration key `bindTo`.
 
#### Deprecated mechanism

You can still use the configuration keys `extraService` and `extraServices`. The underlying implementation will take it
and transform it accordingly.

### Create an extra service

brooklyn-ambari comes with an archetype to bootstrap the creation of a new extra service. Simply run the following
command:

    mvn archetype:generate \
        -DarchetypeGroupId=io.brooklyn.ambari \
        -DarchetypeArtifactId=brooklyn-ambari-service \
        -DarchetypeVersion=0.2.0-SNAPSHOT \
        -DgroupId=<my.groupid> \
        -DartifactId=<my-artifactId>
  
## Extra Stack definitions

If you want brooklyn can install extra stack definitions on the server.  It will accept this in archive 
form e.g. tar, tar.gz, or zip.  These can be located via a url - either http(s)://, classpath://, or file:// 

### Add extra stack definitions to yaml

You can add extra stack definitions using yaml using the ambariStackDefsUrls:

    ...
    services:
    - type: io.brooklyn.ambari.AmbariCluster
      brooklyn.config:
        ...
        ambariStackDefsUrls:
        - https://github.com/abajwa-hw/openldap-stack/archive/master.zip
        
## Ambari server as part of cluster

Brooklyn can install the ambari agent on the same host as the ambari server.
This is only available if you are using the hostgroup approach to defining your cluster in yaml.

### Add ambari component to install on ambari server

If you wish to install ambari components on the server use the serverComponents key:

    ...
    services:
    - type: io.brooklyn.ambari.AmbariCluster
      brooklyn.config:
        ...
        serverComponents:
        - HDFS_CLIENT
