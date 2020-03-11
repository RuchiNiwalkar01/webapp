#!/bin/bash
cd /home/ubuntu
sudo chown -R ubuntu:ubuntu /home/ubuntu/
source /etc/profile
sudo chmod +x /home/ubuntu/webapp-0.0.1-SNAPSHOT.jar
sudo fuser -k 8080/tcp
nohup java -jar /home/ubuntu/webapp-0.0.1-SNAPSHOT.jar > /home/ubuntu/output.txt &