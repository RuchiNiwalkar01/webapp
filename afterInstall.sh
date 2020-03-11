#!/bin/bash
cd /home/ubuntu
sudo chown -R ubuntu:ubuntu /home/ubuntu/
pid=$(ps aux | grep "java -jar" | grep "webapp-0.0.1-SNAPSHOT.jar" | awk '{print $2}')
sudo kill -9 "$pid" 
source /etc/profile
sudo chmod +x /home/ubuntu/webapp-0.0.1-SNAPSHOT.jar
nohup java -jar /home/ubuntu/webapp-0.0.1-SNAPSHOT.jar > /home/ubuntu/output.txt &