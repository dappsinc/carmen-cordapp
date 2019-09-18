# Carmen CRM CorDapp

Carmen is a Customer Relationship Management (CRM) network for inter-firm process automation.

### Carmen CRM Network Setup


1) Install the Carmen CRM CorDapp locally via Git:

```bash

git clone https://gitlab.com/dappsinc/carmen-cordapp

```

2) Deploy the Nodes


```bash

cd carmen-cordapp && gradlew.bat deployNodes (Windows) OR ./gradlew deployNodes (Linux)

```

3) Run the Nodes

```bash

cd workflows
cd build 
cd nodes
runnodes.bat (Windows) OR ./runnodes (Linux)

```
4) Run the Spring Boot Server

```bash

cd ..
cd ..
cd server
../gradlew.bat bootRun -x test (Windows) OR ../gradlew bootRun -x test

```
The Carmen CRM Network API Swagger will be running at `http://localhost:8080/swagger-ui.html#/`

To change the name of your `organisation` or any other parameters, edit the `node.conf` file and repeat the above steps.

### Joining the Network

Add the following to the `node.conf` file:

`compatibilityZoneUrl="http://dsoa.network:8080"`

This is the current network map and doorman server URL for the DSOA Testnet

1) Remove Existing Network Parameters and Certificates

```bash

cd build
cd nodes
cd Dapps
rm -rf persistence.mv.db nodeInfo-* network-parameters certificates additional-node-infos

```

2) Download the Network Truststore

```bash

curl -o /var/tmp/network-truststore.jks http://dsoa.network:8080//network-map/truststore

```

3) Initial Node Registration

```bash

java -jar corda.jar --initial-registration --network-root-truststore /var/tmp/network-truststore.jks --network-root-truststore-password trustpass

```
4) Start the Node

```bash

java -jar corda.jar

```

#### Node Configuration

Configuration 

- Corda version: Corda 4
- Vault SQL Database: PostgreSQL
- Cloud Service Provider: GCP
- JVM or Kubernetes
