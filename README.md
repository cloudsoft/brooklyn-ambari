org.apache.brooklyn.brooklyn-ambari
===

To build an assembly, simply run:

    mvn clean assembly:assembly

This creates a tarball with a full standalone application which can be installed in any *nix machine at:
    target/org.apache.brooklyn.brooklyn-ambari-1.0-SNAPSHOT-dist.tar.gz

It also installs an unpacked version which you can run locally:
 
     cd target/org.apache.brooklyn.brooklyn-ambari-0.1-SNAPSHOT-dist/org.apache.brooklyn.brooklyn-ambari-1.0-SNAPSHOT
     ./start.sh launch

For more information see the README (or `./start.sh help`) in that directory.
On OS X and Linux, this application will deploy to localhost *if* you have key-based 
password-less (and passphrase-less) ssh enabled.

To configure cloud and fixed-IP locations, see the README file in the built application directly.
For more information you can run `./start.sh help`) in that directory.


### Opening in an IDE

To open this project in an IDE, you will need maven support enabled
(e.g. with the relevant plugin).  You should then be able to develop
it and run it as usual.  For more information on IDE support, visit:

    http://brooklyncentral.github.io/dev/build/ide.html

### Sample Blueprint

    name: ambari cluster
    location: jclouds:aws-ec2:eu-west-1
    services:
    - type: org.apache.brooklyn.ambari.ambaricluster
      securityGroup: hdpSecurityGroup
      initialSize: 5

