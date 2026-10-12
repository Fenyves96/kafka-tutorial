################################
##                            ##
##        NO ZOOKEEPER        ##
##                            ##
################################
### Install Java JDK 17 (Amazon Corretto 17)

https://docs.aws.amazon.com/corretto/latest/corretto-11-ug/linux-info.html

##### For Ubuntu 24.04 for example: 
```
wget -O - https://apt.corretto.aws/corretto.key | \
  gpg --dearmor | \
  sudo tee /usr/share/keyrings/corretto-keyring.gpg > /dev/null
echo "deb [signed-by=/usr/share/keyrings/corretto-keyring.gpg] https://apt.corretto.aws stable main" | \
  sudo tee /etc/apt/sources.list.d/corretto.list
sudo apt update
sudo apt install java-17-amazon-corretto-jdk
```
Check the Java version

```
java -version 
```

##### Download Kafka (CLI tools are in the bundle) and setup CLI tools

https://kafka.apache.org/downloads

```
wget https://downloads.apache.org/kafka/4.0.0/kafka_2.13-4.0.0.tgz
```

Extract Kafka
```
tar -xvf kafka_2.13-4.0.0.tgz
```

Move the folder
```
mv kafka_2.13-4.0.0 ~
```

Open the Kafka directory
```
cd kafka_2.13-4.0.0
```

Try out a Kafka command
```
bin/kafka-topics.sh
```

Set PATH variable and add it at the bottom of `~/.bashrc` file to make the scripts easily available

```
nano ~/.bash
```

Add PATH="$PATH:/your/path/to/your/kafka/bin"

Example: PATH="$PATH:~/kafka_2.13-4.0.0/bin"

Open a new terminal and try running the command from any directory:
```
kafka-topics.sh
```

##### create a Kafka directory
```
mkdir kafka-playground
```

##### copy `config/kraft/server.properties` file from the downloaded and extraxted kafka*.tgz file
```
cp <path-of-the-server.properties-file> kafka-playground
```
##### edit server.properties file

    ```
    #  we need to set the controller quorom by adding the following line to the "server.properties" file with "node.id = 1" and a "localhost" listener: 
    controller.quorum.voters=1@localhost:9093

    # also make sure, that on the "listeners" line this is set: 
    CONTROLLER://:9093
    ```

##### generate a Kafka UUID
kafka-storage.sh random-uuid

##### This will format the directory that is in the log.dirs in the server.properties file
kafka-storage.sh format -t <uuid> -c ./server.properties


### start Kafka
```
kafka-server-start.sh ./server.properties
```

### Kafka is running! 
### Keep the terminal window opened